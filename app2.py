from flask import Flask, jsonify, request, session, abort
from sqlalchemy import text, DateTime, func
from flask_cors import CORS, cross_origin
from flask_restful import Api, Resource
from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin, login_user, LoginManager, login_required, logout_user, current_user
from flask_bcrypt import Bcrypt
from sqlalchemy.orm import Mapped
from sqlalchemy.testing.schema import mapped_column
import mysql.connector
import time, logging
import uuid
from sqlalchemy.exc import IntegrityError
from sqlalchemy import and_
import json
from datetime import datetime
from flask_wtf import FlaskForm, CSRFProtect
from wtforms import StringField, PasswordField, SubmitField, IntegerField, BooleanField
from wtforms.validators import InputRequired, Length, ValidationError, Email
import re
from flask_cors import CORS, cross_origin
from sqlalchemy import DateTime, func
from pydantic import BaseModel
import json
from flask_login import UserMixin
from datetime import timedelta
from sqlalchemy import desc
from flask_jwt_extended import JWTManager, jwt_required, create_access_token, get_jwt_identity

app = Flask(__name__)
api = Api(app)
CORS(app)
jwt = JWTManager(app)

# Configure your MySQL database URI
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://root:@localhost/travelbetter-dev'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['WTF_CSRF_ENABLED'] = False
app.secret_key = 'your_secret_key'
app.config['PERMANENT_SESSION_LIFETIME'] = timedelta(minutes=30)

db = SQLAlchemy(app)
login_manager = LoginManager(app)
bcrypt = Bcrypt(app)


@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))


# Test database connection
try:
    with app.app_context():
        db.session.execute(text("SELECT 1"))
        print("Connected to the database successfully!")
except Exception as e:
    print("Failed to connect to the database. Error:", str(e))


def fetch_data_from_sqlalchemy():
    with app.app_context():
        try:
            data = User.query.all()
            return [{'userId': user.userId, 'fullname': user.fullname, 'email': user.email, 'password': user.password}
                    for user in data]
        except Exception as e:
            return {'error': str(e)}


def generate_new_user_id():
    max_id = db.session.query(func.max(User.userId)).scalar()
    return max_id + 1 if max_id is not None else 1


def generate_location_id():
    max_id = db.session.query(func.max(Location.locationId)).scalar()
    return max_id + 1 if max_id is not None else 1


# def generate_vehicle_id():
#     timestamp = int(time.time())
#     return timestamp


# MODELS

class User(db.Model):
    userId = db.Column(db.Integer, primary_key=True)
    firstName = db.Column(db.String(255))
    lastName = db.Column(db.String(255))
    email = db.Column(db.String(255), unique=True, nullable=False)
    contactNumber = db.Column(db.String(255))
    password = db.Column(db.String(255), nullable=False)

    rating = db.Column(db.Integer)
    userType = db.Column(db.Integer)  # 0 = admin, 1 = driver, 2 = student
    isActive = db.Column(db.Boolean, default=False)
    authorized = db.Column(db.Boolean, default=False)
    isQueued = db.Column(db.Boolean, default=False)
    isPetitioned = db.Column(db.Integer, default=0)

    def is_active(self):
        return self.is_active

    def get_id(self):
        return str(self.userId)


class Location(db.Model):
    locationId = db.Column(db.Integer, primary_key=True)
    userId = db.Column(db.Integer)
    latitude = db.Column(db.Float)
    longitude = db.Column(db.Float)
    timestamp = db.Column(DateTime, default=func.now())


class Vehicle(db.Model):
    vehicleId = db.Column(db.Integer, primary_key=True)
    userId = db.Column(db.Integer)
    plateNumber = db.Column(db.String(255))
    route = db.Column(db.String(255))
    isAvailable = db.Column(db.Boolean, default=False)
    hasDeparted = db.Column(db.Boolean, default=False)
    isFull = db.Column(db.Boolean, default=False)
    queuedUsers = db.Column(db.String(255), default="[]")  # Store as JSON string

    def add_queued_user(self, user_id):
        queued_users = json.loads(self.queuedUsers)
        queued_users.append(user_id)
        self.queuedUsers = json.dumps(queued_users)

    def remove_queued_user(self, user_id):
        queued_users = json.loads(self.queuedUsers)
        if user_id in queued_users:
            queued_users.remove(user_id)
            self.queuedUsers = json.dumps(queued_users)


class Petition(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    forestryPetitionCount = db.Column(db.String(255), default="[]")  # Store as JSON string
    ruralPetitionCount = db.Column(db.String(255), default="[]")  # Store as JSON string

    def add_forestry_user(self, user_id):
        forestryPetitionCount = json.loads(self.forestryPetitionCount)
        forestryPetitionCount.append(user_id)
        self.forestryPetitionCount = json.dumps(forestryPetitionCount)


class LoginForm(FlaskForm):
    email = StringField('Email', validators=[InputRequired(), Email(), Length(min=4, max=120)],
                        render_kw={"placeholder": "Email"})
    password = PasswordField(validators=[
        InputRequired(), Length(min=8, max=20)], render_kw={"placeholder": "Password"})
    submit = SubmitField('Login')


# VALIDATION
def is_valid_password(password):
    # Password must be at least 8 characters long
    # Password must contain at least one uppercase letter, one lowercase letter, and one digit
    return bool(re.match(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*-])[A-Za-z\d!@#$%^&*-]+$', password))


def is_valid_email(email):
    # Regular expression for email validation
    email_regex = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return bool(re.match(email_regex, email))


# Define a function to validate input strings
def contains_digits(input_string):
    return bool(re.search(r'\d', input_string))


# ROUTES
@app.route('/register_driver', methods=['POST'])
def register_driver():
    data = request.json

    user_data = data.get('user', {})
    vehicle_data = data.get('vehicle', {})

    firstName = user_data['firstName']
    lastName = user_data['lastName']
    email = user_data['email']
    contactNumber = user_data['contactNumber']
    password = user_data['password']
    plateNumber = vehicle_data['plateNumber']
    route = vehicle_data['route']
    userId = generate_new_user_id()
    vehicleId = userId

    # Check for empty fields
    if not firstName:
        return jsonify({"message": "Please enter your First Name"}), 400
    if not lastName:
        return jsonify({"message": "Please enter your Last Name"}), 400
    if not email:
        return jsonify({"message": "Please enter your Email"}), 400
    if not contactNumber:
        return jsonify({"message": "Please enter your Contact Number"}), 400
    if not password:
        return jsonify({"message": "Please enter your Password"}), 400
    if not plateNumber:
        return jsonify({"message": "Please enter your plate number"}), 400

    # clean data
    adjustedPlateNumber = plateNumber.upper()
    # Validate driver input here
    if contains_digits(firstName):
        return jsonify({"message": "Invalid First Name"}), 400
    if contains_digits(lastName):
        return jsonify({"message": "Invalid Last Name"}), 400
    if not is_valid_email(email):
        return jsonify({"message": "Invalid Email"}), 400
    if not re.match(r'^(\+?\d{1,3})?\s?\d{3}\s?\d{3}\s?\d{4}$', contactNumber):
        print("Invalid Contact Number")
        return jsonify({"message": "Invalid Contact Number"}), 400
    if not is_valid_password(password):
        return jsonify({"message": "Invalid Password"}), 400
    if not re.match(r'^[A-Z0-9]{3}\s\d{3,4}$', adjustedPlateNumber):
        print("Invalid Plate Number")
        return jsonify({"message": "Invalid Plate Number"}), 400

    existing_user = User.query.filter_by(email=email).first()
    if existing_user:
        return jsonify({'message': 'Email already registered'}), 400

    existing_plate_number = Vehicle.query.filter_by(plateNumber=adjustedPlateNumber).first()
    if existing_plate_number:
        print('Plate number already registered')
        return jsonify({'message': 'Plate number already registered'}), 400

    adjusted_firstName = firstName.capitalize()
    adjusted_lastName = lastName.capitalize()
    hashed_password = bcrypt.generate_password_hash(password)
    new_user = User(userId=userId, firstName=adjusted_firstName, lastName=adjusted_lastName,
                    email=email, contactNumber=contactNumber, password=hashed_password,
                    rating=0, userType=2, isActive=0)

    new_vehicle = Vehicle(vehicleId=vehicleId,
                          userId=userId,
                          plateNumber=adjustedPlateNumber,
                          route=route)

    db.session.add(new_vehicle)
    db.session.add(new_user)

    db.session.commit()
    return jsonify({'message': 'User registered successfully'})


@app.route('/register_user', methods=['POST'])
def register_user():
    # Check for empty fields
    firstName = request.json['firstName']
    lastName = request.json['lastName']
    email = request.json['email']
    contactNumber = request.json['contactNumber']
    password = request.json['password']

    if not firstName:
        return jsonify({"message": "Please enter your First Name"}), 400
    if not lastName:
        return jsonify({"message": "Please enter your Last Name"}), 400
    if not email:
        return jsonify({"message": "Please enter your Email"}), 400
    if not contactNumber:
        return jsonify({"message": "Please enter your Contact Number"}), 400
    if not password:
        return jsonify({"message": "Please enter your Password"}), 400

    # Validate user input here
    if contains_digits(firstName):
        return jsonify({"message": "Invalid First Name"}), 400
    if contains_digits(lastName):
        return jsonify({"message": "Invalid Last Name"}), 400
    if not is_valid_email(email):
        return jsonify({"message": "Invalid Email"}), 400
    if not re.match(r'^(\+?\d{1,3})?\s?\d{3}\s?\d{3}\s?\d{4}$', contactNumber):
        return jsonify({"message": "Invalid Contact Number"}), 400
    if not is_valid_password(password):
        return jsonify({"message": "Invalid Password"}), 400

    # check if email is unique
    email = email.lower()
    existing_user = User.query.filter_by(email=email).first()
    if existing_user:
        return jsonify({'message': 'Email already registered'}), 400

    userId = generate_new_user_id()
    hashed_password = bcrypt.generate_password_hash(password)
    adjusted_firstName = firstName.capitalize()
    adjusted_lastName = lastName.capitalize()
    new_user = User(userId=userId, firstName=adjusted_firstName, lastName=adjusted_lastName,
                    email=email, contactNumber=contactNumber, password=hashed_password,
                    rating=0, userType=1, isActive=0)

    db.session.add(new_user)
    db.session.commit()
    return jsonify({'message': 'User registered successfully'})


@app.route('/edit_user/<int:userId>', methods=['PUT'])
def edit_user(userId):
    # Assuming you have some function to retrieve the user from the database
    user = User.query.get(userId)
    vehicle = Vehicle.query.filter_by(userId=userId).first()

    # Update attributes if provided in the request
    if request.json['firstName'] != '':
        if contains_digits(request.json['firstName']):
            return jsonify({"message": "Invalid First Name"}), 400
        adjusted_firstName = request.json['firstName'].capitalize()
        user.firstName = adjusted_firstName

    if request.json['lastName'] != '':
        if contains_digits(request.json['lastName']):
            return jsonify({"message": "Invalid Last Name"}), 400
        adjusted_lastName = request.json['lastName'].capitalize()
        user.lastName = adjusted_lastName

    if request.json['email'] != '':
        if is_valid_email(request.json['email']):
            # check if email is unique
            email = request.json['email'].lower()
            existing_user = User.query.filter_by(email=email).first()
            if existing_user:
                return jsonify({'message': 'Email already registered'}), 400
            user.email = request.json['email']
        else:
            return jsonify({"message": "Invalid Email"}), 400

    if request.json['contactNumber'] != '':
        contact_number = request.json['contactNumber']
        if re.match(r'^(\+?\d{1,3})?\s?\d{3}\s?\d{3}\s?\d{4}$', contact_number):
            user.contactNumber = contact_number
        else:
            return jsonify({"message": "Invalid Contact Number"}), 400

    if request.json['password'] != '':
        if is_valid_password(request.json['password']):
            hashed_password = bcrypt.generate_password_hash(request.json['password'])
            user.password = hashed_password
        else:
            return jsonify({"message": "Invalid Password"}), 400

    if request.json['plateNumber'] != '':
        plate_number = request.json['plateNumber']
        adjustedPlateNumber = plate_number.upper()
        if re.match(r'^[A-Z0-9]{3}\s\d{3,4}$', adjustedPlateNumber):
            vehicle.plateNumber = adjustedPlateNumber
        else:
            return jsonify({"message": "Invalid Plate Number"}), 400

    if request.json['route'] != '':
        vehicle.route = request.json['route']

    db.session.commit()

    return jsonify({"message": "User successfully edited"}), 200


@app.route('/login', methods=['POST'])
def login():
    email = request.json.get('email')
    password = request.json.get('password')
    print("EMAIL: ", email)
    user = User.query.filter_by(email=email).first()
    if user:
        if bcrypt.check_password_hash(user.password, password):
            session['user_id'] = user.userId
            access_token = create_access_token(identity=user.userId)
            login_user(user)
            return jsonify({
                'accessToken': access_token
            })
        else:
            return jsonify({'error': 'Invalid password'}), 401
    else:
        return jsonify({'error': 'User not found'}), 404


# @app.route('/google-sign-in-endpoint', methods=['POST'])
# def google_sign_in():
#     YOUR_WEB_CLIENT_ID = "AIzaSyD9BAhnNb62l_L_Htwtf3uJ1Q-saSoFFtw"
#     token = request.json.get('idToken')
#     try:
#         # Validate the received ID token
#         idinfo = id_token.verify_oauth2_token(token, requests.Request(), YOUR_WEB_CLIENT_ID)
#
#         # Extract user information
#         user_id = idinfo['sub']
#         user_email = idinfo['email']
#         # You can extract more user information as needed
#
#         # Process the user data (e.g., create a session, store in database)
#         # Example: session['user_id'] = user_id
#         session['user_id'] = new_user.id
#         access_token = create_access_token(identity=user.userId)
#
#         # Respond with a success message or user data
#         return jsonify({'accessToken': access_token}), 200
#
#     except ValueError:
#         # Invalid token
#         return jsonify({'error': 'Invalid token'}), 401

@app.route('/google-sign-up-endpoint', methods=['POST'])
def google_sign_up():
    YOUR_WEB_CLIENT_ID = "562377295927-26r2kaucq403vbpo01pd5bjq9volo46n.apps.googleusercontent.com"
    print("SIGN UP THROUGH GOOGLE")
    token = request.json.get('accessToken')

    try:
        # Validate the received ID token
        idinfo = id_token.verify_oauth2_token(token, requests.Request(), YOUR_WEB_CLIENT_ID)

        # Extract user information
        user_email = idinfo['email']

        # Check if the user already exists in your database
        existing_user = User.query.filter_by(email=user_email).first()

        if existing_user:
            # If the user already exists, return an error or redirect to sign-in page
            return jsonify({'error': 'User already exists'}), 400
        else:
            # Create a new user account
            print("NEW USER THROUGH GOOGLE")
            new_user = User(email=user_email)
            # You can also set other user attributes here
            # For example: new_user.name = idinfo['name']

            # Save the new user to the database
            db.session.add(new_user)
            db.session.commit()

            # Log in the new user
            session['user_id'] = new_user.id
            access_token = create_access_token(identity=user.userId)

            return jsonify({'accessToken': access_token}), 200

    except ValueError:
        # Invalid token
        return jsonify({'error': 'Invalid token'}), 401


@app.route('/protected', methods=['GET'])
@jwt_required()
def protected():
    # Access protected resource
    current_user = get_jwt_identity()
    return jsonify(logged_in_as=current_user), 200


@app.route('/data', methods=['GET'])
@jwt_required()
def get_data():
    current_user = get_jwt_identity()
    user = User.query.filter_by(userId=current_user).first()
    if user:
        return jsonify({
            'message': 'User logged in successfully',
            'user': {
                'userId': user.userId,
                'firstName': user.firstName,
                'lastName': user.lastName,
                'email': user.email,
                'contactNumber': user.contactNumber,
                'password': user.password,
                'userType': user.userType,
                'isQueued': user.isQueued,
                'isPetitioned': user.isPetitioned,
            }
        })
    else:
        return jsonify({'error'}), 401


@app.route("/logout")
@cross_origin(supports_credentials=True)
@login_required
def logout():
    session.pop('user_id', None)
    logout_user()
    return jsonify({'message': 'Logged out'})


@app.route('/add_queued_user/<int:vehicleId>/<int:userId>', methods=['POST'])
def add_queued_user(vehicleId, userId):
    try:
        vehicle = Vehicle.query.get(vehicleId)
        user = User.query.get(userId)

        print("Vehicle ID: ", vehicleId)
        print("User ID: ", userId)

        if vehicle is None:
            return jsonify({'error': f'Vehicle with ID {vehicleId} not found'}), 404

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

            # Check if the user is already queued
        if user.isQueued:
            return jsonify({'error': f'User {userId} is already queued to another vehicle'}), 400

        user.isQueued = True
        vehicle.add_queued_user(userId)
        db.session.commit()

        return jsonify({'message': f'User {userId} added to the queued users list for vehicle {vehicleId}'}), 200
    except Exception as e:
        print(f"Error occurred while processing vehicle with ID {vehicleId}: {e}")
        # Handle any unexpected errors
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


@app.route('/remove_queued_user/<int:vehicleId>/<int:userId>', methods=['PUT', 'DELETE'])
def remove_queued_user(vehicleId, userId):
    try:
        vehicle = Vehicle.query.get(vehicleId)
        user = User.query.get(userId)

        if vehicle is None:
            return {'error': f'Vehicle with ID {vehicleId} not found'}, 404

        if user is None:
            return {'error': f'User with ID {userId} not found'}, 404

        # Convert queuedUsers from JSON string to list
        queued_users_list = json.loads(vehicle.queuedUsers)

        if userId not in queued_users_list:
            return {'error': f'User {userId} is not queued for vehicle {vehicleId}'}, 404

        # Remove userId from the list
        queued_users_list.remove(userId)

        # Convert the list back to JSON string
        vehicle.queuedUsers = json.dumps(queued_users_list)
        user.isQueued = not user.isQueued
        db.session.commit()

        return {
                   'message': f'User {userId} removed from the queued users list of vehicle {vehicleId} and is no longer queued'}, 200
    except ValueError:
        return {'error': 'Invalid vehicleId or userId. Please provide integer values.'}, 400
    except Exception as e:
        logging.error(f'An error occurred: {str(e)}')
        return {'error': 'An unexpected error occurred.'}, 500


@app.route('/change_is_queued/<int:userId>', methods=['PUT'])
def change_is_queued(userId):
    try:
        user = User.query.get(userId)

        if user is None:
            return {'error': f'User with ID {userId} not found'}, 404

        # Toggle the value of isQueued
        user.isQueued = not user.isQueued
        db.session.commit()
        return {
                   'message': f'User {userId} has changed is queued value'}, 200

    except Exception as e:
        return {'error': f'An error occurred: {str(e)}'}, 500


@app.route('/is_authorized/<int:userId>', methods=['GET'])
def is_authorized(userId):
    try:
        user = User.query.get(userId)
        if user.authorized:
            return jsonify(True)
        else:
            return jsonify(False)
    except Exception as e:
        return {'error': f'An error occurred: {str(e)}'}, 500


@app.route('/get_is_queued/<int:userId>', methods=['GET'])
def get_is_queued(userId):
    try:
        user = User.query.get(userId)

        if user is None:
            return {'error': f'User with ID {userId} not found'}, 404

        if user.isQueued:
            return jsonify(True)
        else:
            return jsonify(False)
    except Exception as e:
        return {'error': f'An error occurred: {str(e)}'}, 500


@app.route('/add_user', methods=['POST'])
def add_user():
    data = request.get_json()

    userId = data.get('userId')
    fullname = data.get('fullname')
    userType = data.get('userType')
    locationId = data.get('locationId')

    new_user = User(userId=userId, fullname=fullname, userType=userType, locationId=locationId)

    db.session.add(new_user)
    db.session.commit()
    return jsonify({'message': 'User added successfully'})


@app.route('/add_location', methods=['POST'])
@jwt_required()
def add_location():
    userId = get_jwt_identity()
    data = request.get_json()
    locationId = generate_location_id()
    latitude = data.get('latitude')
    longitude = data.get('longitude')
    timestamp = datetime.now()

    num_locations = Location.query.filter_by(userId=userId).count()

    # If the number of locations is greater than 10, delete the oldest locations
    if num_locations >= 5:
        oldest_locations = Location.query.filter_by(userId=userId).order_by(desc(Location.timestamp)).offset(5).all()
        for location in oldest_locations:
            db.session.delete(location)
        db.session.commit()

    new_location = Location(locationId=locationId, userId=userId, latitude=latitude, longitude=longitude,
                            timestamp=timestamp)

    db.session.add(new_location)
    db.session.commit()
    return jsonify({'message': 'Location added successfully'})


@app.route('/update_authorized/<int:userId>', methods=['PUT'])
def update_authorized(userId):
    user = User.query.get(userId)
    user.authorized = True
    db.session.commit()
    return jsonify({'message': 'User authorized successfully'})


@app.route('/ready_driver', methods=['PUT'])
@jwt_required()
def ready_driver():
    userId = get_jwt_identity()
    user = User.query.get(userId)
    if user:
        vehicle = Vehicle.query.filter_by(userId=user.userId).first()
        if vehicle:
            vehicle.isAvailable = True
            db.session.commit()
            return jsonify({'message': 'Driver set to ready successfully'})
        else:
            return jsonify({'message': 'No vehicle found for this user'}), 404
    return jsonify({'message': 'Driver set to ready successfully'})


@app.route('/get_incoming_passengers', methods=['GET'])
@jwt_required()
def get_incoming_passengers():
    userId = get_jwt_identity()
    user = User.query.get(userId)
    vehicle = Vehicle.query.filter_by(userId=user.userId).first()
    if vehicle:
        queued_users_count = len(json.loads(vehicle.queuedUsers))
        return str(queued_users_count)
    else:
        return jsonify({'error': 'Vehicle not found'})


# @app.route('/get_locations', methods=['GET'])
# def get_locations():
#     # Subquery to get the latest timestamp for each user with user type 2
#     latest_timestamps = db.session.query(Location.userId, func.max(Location.timestamp).label('max_timestamp')) \
#         .join(User, Location.userId == User.userId) \
#         .join(Vehicle, Vehicle.userId == User.userId) \
#         .filter(User.userType == 2) \
#         .filter(Vehicle.isAvailable == True) \
#         .group_by(Location.userId) \
#         .subquery()
#
#     # Query to get the latest location for each user with user type 2 and isAvailable vehicles
#     latest_locations = db.session.query(Location) \
#         .join(latest_timestamps,
#               (Location.userId == latest_timestamps.c.userId) &
#               (Location.timestamp == latest_timestamps.c.max_timestamp)) \
#         .all()
#
#     # Construct JSON response
#     response_data = [{'locationId': loc.locationId,
#                       'userId': loc.userId,
#                       'latitude': loc.latitude,
#                       'longitude': loc.longitude} for loc in latest_locations]
#
#     print(response_data)
#
#     return jsonify(response_data)

@app.route('/get_locations', methods=['GET'])
@jwt_required()
def get_locations():
    # Subquery to get the latest timestamp for each user with user type 2
    latest_timestamps_subquery = db.session.query(Location.userId, func.max(Location.timestamp).label('max_timestamp')) \
        .group_by(Location.userId) \
        .subquery()

    # Query to get the latest location for each user with user type 2 and isAvailable vehicles
    locations = db.session.query(Location, Vehicle) \
        .join(Vehicle, Vehicle.userId == Location.userId) \
        .join(User, User.userId == Location.userId) \
        .join(latest_timestamps_subquery,
              and_(Location.userId == latest_timestamps_subquery.c.userId,
                   Location.timestamp == latest_timestamps_subquery.c.max_timestamp)) \
        .filter(Vehicle.isAvailable == True,
                User.userType == 2) \
        .all()

    # Construct JSON response
    locations_dict = [{
        'locationId': location.locationId,
        'userId': location.userId,
        'latitude': location.latitude,
        'longitude': location.longitude,
        'plateNumber': vehicle.plateNumber,
    } for location, vehicle in locations]

    print(locations_dict)
    return jsonify(locations_dict)

@app.route('/get_available_forestry_drivers', methods=['GET'])
@jwt_required()
def get_available_forestry_drivers():
    subquery = db.session.query(Location.userId, func.max(Location.timestamp).label('latest_timestamp')).group_by(
        Location.userId).subquery()
    drivers = db.session.query(User, Vehicle, Location).join(Vehicle, User.userId == Vehicle.userId).join(Location,
                                                                                                          User.userId == Location.userId).filter(
        User.userType == 2,
        User.authorized.is_(True),
        Vehicle.route == 'Forestry',
        Vehicle.isAvailable.is_(True),
        Location.timestamp == subquery.c.latest_timestamp).all()

    drivers_dict = [{
        'userId': user.userId,
        'vehicleId': vehicle.vehicleId,
        'firstName': user.firstName,
        'lastName': user.lastName,
        'contactNumber': user.contactNumber,
        'rating': user.rating,
        'userType': user.userType,
        'isActive': user.isActive,
        'authorized': user.authorized,
        'plateNumber': vehicle.plateNumber,
        'route': vehicle.route,
        'isAvailable': vehicle.isAvailable,
        'hasDeparted': vehicle.hasDeparted,
        'isFull': vehicle.isFull,
        'queuedUsers': vehicle.queuedUsers if isinstance(vehicle.queuedUsers, list) else [],
        'latitude': location.latitude,
        'longitude': location.longitude
    } for user, vehicle, location in drivers]

    return jsonify(drivers_dict)


@app.route('/get_available_rural_drivers', methods=['GET'])
@jwt_required()
def get_available_rural_drivers():
    drivers = db.session.query(User, Vehicle, Location).join(Vehicle, User.userId == Vehicle.userId).join(Location,
                                                                                                          User.userId == Location.userId).filter(
        User.userType == 2,
        User.authorized.is_(True),
        Vehicle.route == 'Rural',
        Vehicle.isAvailable.is_(True)).all()

    drivers_dict = [{
        'userId': user.userId,
        'vehicleId': vehicle.vehicleId,
        'firstName': user.firstName,
        'lastName': user.lastName,
        'contactNumber': user.contactNumber,
        'rating': user.rating,
        'userType': user.userType,
        'isActive': user.isActive,
        'authorized': user.authorized,
        'plateNumber': vehicle.plateNumber,
        'route': vehicle.route,
        'isAvailable': vehicle.isAvailable,
        'hasDeparted': vehicle.hasDeparted,
        'isFull': vehicle.isFull,
        'queuedUsers': vehicle.queuedUsers if isinstance(vehicle.queuedUsers, list) else [],
        'latitude': location.latitude,
        'longitude': location.longitude
    } for user, vehicle, location in drivers]

    return jsonify(drivers_dict)

@app.route('/get_users', methods=['GET'])
@jwt_required()
def get_users():
    users = db.session.query(User).filter(User.userType == 1).all()

    users_dict = [{
        'userId': user.userId,
        'firstName': user.firstName,
        'lastName': user.lastName,
        'contactNumber': user.contactNumber,
        'email': user.email,

    } for user in users]

    print(users_dict)

    return jsonify(users_dict)

@app.route('/get_auth_drivers', methods=['GET'])
@jwt_required()
def get_auth_drivers():
    drivers = db.session.query(User, Vehicle).join(Vehicle, User.userId == Vehicle.userId).filter(
        User.userType == 2,
        User.authorized.is_(True)).all()

    drivers_dict = [{
        'userId': user.userId,
        'firstName': user.firstName,
        'lastName': user.lastName,
        'contactNumber': user.contactNumber,
        'email': user.email,
        'rating': user.rating,
        'userType': user.userType,
        'isActive': user.isActive,
        'authorized': user.authorized,
        'plateNumber': vehicle.plateNumber,
        'route': vehicle.route,
        'isAvailable': vehicle.isAvailable,
        'hasDeparted': vehicle.hasDeparted,
        'isFull': vehicle.isFull,
        'queuedUsers': vehicle.queuedUsers if isinstance(vehicle.queuedUsers, list) else []
    } for user, vehicle in drivers]

    return jsonify(drivers_dict)


@app.route('/get_pending_drivers', methods=['GET'])
@jwt_required()
def get_pending_drivers():
    drivers = db.session.query(User, Vehicle).join(Vehicle, User.userId == Vehicle.userId).filter(
        User.userType == 2,
        User.authorized.is_(False)).all()

    drivers_dict = [{
        'userId': user.userId,
        'firstName': user.firstName,
        'lastName': user.lastName,
        'contactNumber': user.contactNumber,
        'rating': user.rating,
        'userType': user.userType,
        'isActive': user.isActive,
        'authorized': user.authorized,
        'plateNumber': vehicle.plateNumber,
        'route': vehicle.route,
        'isAvailable': vehicle.isAvailable,
        'hasDeparted': vehicle.hasDeparted,
        'isFull': vehicle.isFull,
        'queuedUsers': vehicle.queuedUsers if isinstance(vehicle.queuedUsers, list) else []

    } for user, vehicle in drivers]

    return jsonify(drivers_dict)


@app.route('/admin_delete_user', methods=['DELETE'])
def admin_delete_user():
    try:
        userId = get_jwt_identity()
        # Step 1: Delete associated vehicle records
        Vehicle.query.filter_by(userId=userId).delete()
        Location.query.filter_by(userId=userId).delete()
        # Step 2: Delete the user record
        user = User.query.get(userId)
        db.session.delete(user)
        db.session.commit()
        return "User and associated vehicles deleted successfully"
    except IntegrityError:
        db.session.rollback()
        return "Integrity error occurred while deleting user and associated vehicles"
    except Exception as e:
        db.session.rollback()
        return f"An error occurred: {str(e)}"


@app.route('/take_passengers', methods=['PUT'])
@jwt_required()
def take_passengers():
    try:
        userId = get_jwt_identity()
        user = User.query.get(userId)
        petition = Petition.query.filter_by(id=1).first()
        vehicle = Vehicle.query.filter_by(userId=userId).first()

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

        if petition:
            if vehicle.route == 'Forestry':
                forestry_petition_count_array = json.loads(petition.forestryPetitionCount)
                array_to_transfer = forestry_petition_count_array
                petition.forestryPetitionCount = json.dumps([])  # Setting an empty array
                User.query.filter_by(isPetitioned=1).update({'isPetitioned': 0}, synchronize_session=False)
            else:
                rural_petition_count_array = json.loads(petition.ruralPetitionCount)
                array_to_transfer = rural_petition_count_array
                petition.ruralPetitionCount = json.dumps([])  # Setting an empty array
                User.query.filter_by(isPetitioned=2).update({'isPetitioned': 0}, synchronize_session=False)

            # Convert the array to a JSON string
            queued_users_json = json.dumps(array_to_transfer)

            # Update the queuedUsers field in the Vehicle model
            vehicle.queuedUsers = queued_users_json
            db.session.commit()
            return jsonify({'message': f'User {userId} added to forestry petition'}), 200
        else:
            return jsonify({'error': 'Forestry petition not found'}), 404
    except Exception as e:
        print(f"Error occurred while processing: {str(e)}")
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


@app.route('/add_forestry_petition', methods=['PUT'])
@jwt_required()
def add_forestry_petition():
    try:
        userId = get_jwt_identity()
        user = User.query.get(userId)
        petition = Petition.query.filter_by(id=1).first()

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

        if user.isPetitioned == 1:  # Assuming isPetitioned is an attribute of the User model
            return jsonify({'error': f'User {userId} is already petitioned to another route'}), 400

        if petition:
            forestry_petition_counts = json.loads(petition.forestryPetitionCount)
            forestry_petition_counts.append(userId)
            petition.forestryPetitionCount = json.dumps(forestry_petition_counts)
            user.isPetitioned = 1
            db.session.commit()
            return jsonify({'message': f'User {userId} added to forestry petition'}), 200
        else:
            return jsonify({'error': 'Forestry petition not found'}), 404
    except Exception as e:
        print(f"Error occurred while processing: {str(e)}")
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


@app.route('/add_rural_petition', methods=['PUT'])
@jwt_required()
def add_rural_petition():
    try:
        userId = get_jwt_identity()
        user = User.query.get(userId)
        petition = Petition.query.filter_by(id=1).first()

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

        if user.isPetitioned == 2:  # Assuming isPetitioned is an attribute of the User model
            return jsonify({'error': f'User {userId} is already petitioned to another route'}), 400

        if petition:
            rural_petition_counts = json.loads(petition.ruralPetitionCount)
            rural_petition_counts.append(userId)
            petition.ruralPetitionCount = json.dumps(rural_petition_counts)
            user.isPetitioned = 2
            db.session.commit()
            return jsonify({'message': f'User {userId} added to forestry petition'}), 200
        else:
            return jsonify({'error': 'Forestry petition not found'}), 404
    except Exception as e:
        print(f"Error occurred while processing: {str(e)}")
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


@app.route('/delete_petition', methods=['DELETE'])
@jwt_required()
def delete_petition():
    try:
        userId = get_jwt_identity()
        user = User.query.get(userId)
        petition = Petition.query.filter_by(id=1).first()

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

        if petition:
            if user.isPetitioned == 1:
                # Forestry
                forestry_petition_counts = json.loads(petition.forestryPetitionCount)
                if userId in forestry_petition_counts:
                    forestry_petition_counts.remove(userId)  # Remove the user ID from the list
                    petition.forestryPetitionCount = json.dumps(forestry_petition_counts)
                    user.isPetitioned = 0
                    db.session.commit()
                    return jsonify({'message': f'User {userId} removed from forestry petition'}), 200
                else:
                    return jsonify({'error': f'User {userId} not found in forestry petition'}), 404
            else:
                # Rural
                rural_petition_counts = json.loads(petition.ruralPetitionCount)
                if userId in rural_petition_counts:
                    rural_petition_counts.remove(userId)  # Remove the user ID from the list
                    petition.ruralPetitionCount = json.dumps(rural_petition_counts)
                    user.isPetitioned = 0
                    db.session.commit()
                    return jsonify({'message': f'User {userId} removed from forestry petition'}), 200
                else:
                    return jsonify({'error': f'User {userId} not found in forestry petition'}), 404
        else:
            return jsonify({'error': 'Forestry petition not found'}), 404
    except Exception as e:
        print(f"Error occurred while processing: {str(e)}")
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


@app.route('/get_forestry_petition', methods=['GET'])
@jwt_required()
def get_forestry_petition():
    try:
        userId = get_jwt_identity()
        user = User.query.get(userId)
        petition = Petition.query.filter_by(id=1).first()

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

        if petition:
            forestry_petition_counts = json.loads(petition.forestryPetitionCount)
            count = len(forestry_petition_counts)  # Get count of items in the array
            print("COUNT: ", count)
            return str(count), 200
        else:
            return jsonify({'error': 'Forestry petition not found'}), 404
    except Exception as e:
        print(f"Error occurred while processing: {str(e)}")
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


@app.route('/get_rural_petition', methods=['GET'])
@jwt_required()
def get_rural_petition():
    try:
        userId = get_jwt_identity()
        user = User.query.get(userId)
        petition = Petition.query.filter_by(id=1).first()

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

        if petition:
            if user.route == 'Forestry':
                forestry_petition_counts = json.loads(petition.forestryPetitionCount)
                count = len(forestry_petition_counts)  # Get count of items in the array
            else:
                rural_petition_counts = json.loads(petition.ruralPetitionCount)
                count = len(rural_petition_counts)  # Get count of items in the array
            print("COUNT: ", count)
            return str(count), 200
        else:
            return jsonify({'error': 'Forestry petition not found'}), 404
    except Exception as e:
        print(f"Error occurred while processing: {str(e)}")
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


@app.route('/get_petition', methods=['GET'])
@jwt_required()
def get_petition():
    try:
        userId = get_jwt_identity()
        user = User.query.get(userId)
        petition = Petition.query.filter_by(id=1).first()

        if user is None:
            return jsonify({'error': f'User with ID {userId} not found'}), 404

        if petition:
            forestry_petition_counts = json.loads(petition.forestryPetitionCount)
            count = len(forestry_petition_counts)  # Get count of items in the array
            print("COUNT: ", count)
            return str(count), 200
        else:
            return jsonify({'error': 'Forestry petition not found'}), 404
    except Exception as e:
        print(f"Error occurred while processing: {str(e)}")
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


if __name__ == '__main__':
    print("Flask Server")
    app.run(host='0.0.0.0', port=5000, debug=True)
