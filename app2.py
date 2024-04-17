from flask import Flask, jsonify, request, session
from sqlalchemy import text, DateTime, func
from flask_cors import CORS, cross_origin
from flask_restful import Api, Resource
from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin, login_user, LoginManager, login_required, logout_user, current_user
from flask_bcrypt import Bcrypt
from sqlalchemy.orm import Mapped
from sqlalchemy.testing.schema import mapped_column
import mysql.connector
import time
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

app = Flask(__name__)
api = Api(app)
CORS(app)

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


# FORMS

class RegisterForm(FlaskForm):
    email = StringField('Email', validators=[InputRequired(), Email(), Length(min=4, max=120)],
                        render_kw={"placeholder": "Email"})
    firstName = StringField('First Name', validators=[InputRequired(), Length(min=2, max=255)],
                            render_kw={"placeholder": "First Name"})
    lastName = StringField('Last Name', validators=[InputRequired(), Length(min=2, max=255)],
                           render_kw={"placeholder": "Last Name"})
    contactNumber = StringField('Contact Number', validators=[InputRequired()],
                                render_kw={"placeholder": "Contact Number"})
    password = PasswordField(validators=[
        InputRequired(), Length(min=8, max=20)], render_kw={"placeholder": "Password"})
    rating = IntegerField('Rating')
    userType = IntegerField('User Type')
    isActive = BooleanField('Is Active', validators=[InputRequired()],
                            render_kw={"placeholder": "Is Active"})
    if userType == 2:
        plateNumber = StringField('Last Name', validators=[InputRequired(), Length(min=2, max=255)],
                                  render_kw={"placeholder": "Plate Number"})
        route = StringField('Last Name', validators=[InputRequired(), Length(min=2, max=255)],
                            render_kw={"placeholder": "Route"})

    submit = SubmitField('Register')

    def validate_email(self, email):
        existing_user_email = User.query.filter_by(
            email=email.data).first()
        if existing_user_email:
            raise ValidationError(
                'That email already exists. Please choose a different one.')

    def validate_contactNumber(self, contactNumber):
        # Adjust the pattern if necessary to match specific formats
        ph_number_pattern = re.compile(r'^(09|\+639|\+63 9)[0-9]{2}-?[0-9]{3}-?[0-9]{4}$')
        if not ph_number_pattern.match(contactNumber.data):
            raise ValidationError(
                'Invalid Philippine contact number format. Please use formats like 09171234567 or +639171234567.')


class LoginForm(FlaskForm):
    email = StringField('Email', validators=[InputRequired(), Email(), Length(min=4, max=120)],
                        render_kw={"placeholder": "Email"})
    password = PasswordField(validators=[
        InputRequired(), Length(min=8, max=20)], render_kw={"placeholder": "Password"})
    submit = SubmitField('Login')


class RegisterFormDriver(FlaskForm):
    email = StringField('Email', validators=[InputRequired(), Email(), Length(min=4, max=120)],
                        render_kw={"placeholder": "Email"})
    firstName = StringField('First Name', validators=[InputRequired(), Length(min=2, max=255)],
                            render_kw={"placeholder": "First Name"})
    lastName = StringField('Last Name', validators=[InputRequired(), Length(min=2, max=255)],
                           render_kw={"placeholder": "Last Name"})
    contactNumber = StringField('Contact Number', validators=[InputRequired()],
                                render_kw={"placeholder": "Contact Number"})
    password = PasswordField(validators=[
        InputRequired(), Length(min=8, max=20)], render_kw={"placeholder": "Password"})
    rating = IntegerField('Rating')
    userType = IntegerField('User Type')
    isActive = BooleanField('Is Active', validators=[InputRequired()],
                            render_kw={"placeholder": "Is Active"})
    plateNumber = StringField('Last Name', validators=[InputRequired(), Length(min=2, max=255)],
                              render_kw={"placeholder": "Plate Number"})
    route = StringField('Last Name', validators=[InputRequired(), Length(min=2, max=255)],
                        render_kw={"placeholder": "Route"})

    submit = SubmitField('Register')

    def validate_email(self, email):
        existing_user_email = User.query.filter_by(
            email=email.data).first()
        if existing_user_email:
            raise ValidationError(
                'That email already exists. Please choose a different one.')

    def validate_contactNumber(self, contactNumber):
        # Adjust the pattern if necessary to match specific formats
        ph_number_pattern = re.compile(r'^(09|\+639|\+63 9)[0-9]{2}-?[0-9]{3}-?[0-9]{4}$')
        if not ph_number_pattern.match(contactNumber.data):
            raise ValidationError(
                'Invalid Philippine contact number format. Please use formats like 09171234567 or +639171234567.')


# ROUTES

@app.route('/register_driver', methods=['POST'])
def register_driver():
    data = request.json

    user_data = data.get('user', {})
    vehicle_data = data.get('vehicle', {})

    # form = RegisterFormDriver()
    # if form.validate_on_submit():
    #     userId = generate_new_user_id()
    #     hashed_password = bcrypt.generate_password_hash(form.password.data)
    #     new_user = User(userId=userId, firstName=form.firstName.data, lastName=form.lastName.data,
    #                     email=form.email.data, contactNumber=form.contactNumber.data, password=hashed_password,
    #                     rating=form.rating.data, userType=form.userType.data, isActive=form.isActive.data)
    #
    #     vehicleId = generate_vehicle_id()
    #     new_vehicle = Vehicle(vehicleId=vehicleId,
    #                           userId=userId,
    #                           plateNumber=form.plateNumber.data,
    #                           route=form.route.data)

    userId = generate_new_user_id()
    hashed_password = bcrypt.generate_password_hash(user_data['password'])
    new_user = User(userId=userId, firstName=user_data['firstName'], lastName=user_data['lastName'],
                    email=user_data['email'], contactNumber=user_data['contactNumber'], password=hashed_password,
                    rating=user_data['rating'], userType=user_data['userType'], isActive=user_data['isActive'])

    vehicleId = generate_location_id
    new_vehicle = Vehicle(vehicleId=vehicleId,
                          userId=userId,
                          plateNumber=vehicle_data['plateNumber'],
                          route=vehicle_data['route'])

    db.session.add(new_vehicle)
    db.session.add(new_user)

    db.session.commit()
    return jsonify({'message': 'User registered successfully'})
    # else:
    #     print(form.errors)
    #     return jsonify({'error': 'Invalid registration data'}), 400


@app.route('/register', methods=['POST'])
def register():
    form = RegisterForm()
    if form.validate_on_submit():
        userId = generate_new_user_id()
        hashed_password = bcrypt.generate_password_hash(form.password.data)
        new_user = User(userId=userId, firstName=form.firstName.data, lastName=form.lastName.data,
                        email=form.email.data, contactNumber=form.contactNumber.data, password=hashed_password,
                        rating=form.rating.data, userType=form.userType.data, isActive=form.isActive.data)

        if form.userType.data == 2:
            vehicleId = generate_vehicle_id()
            new_vehicle = Vehicle(vehicleId=vehicleId,
                                  userId=userId,
                                  plateNumber=form.plateNumber.data,
                                  route=form.route.data, )
            db.session.add(new_vehicle)

        db.session.add(new_user)
        db.session.commit()
        return jsonify({'message': 'User registered successfully'})
    else:
        print(form.errors)
        return jsonify({'error': 'Invalid registration data'}), 400


@app.route('/signin', methods=['POST'])
@cross_origin(supports_credentials=True)
def sign_in():
    form = LoginForm()
    print("Form data:", form.email.data, form.password.data)
    if form.validate_on_submit():
        user = User.query.filter_by(email=form.email.data).first()
        if user:
            if bcrypt.check_password_hash(user.password, form.password.data):
                session['user_id'] = user.userId
                login_user(user)
                print("Session contents:", session)
                print("SESSION USER ID: ", session.get('user_id'))
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
                    }
                })
            else:
                print("FAILED TO LOG IN")
                return jsonify({'error': 'Invalid username or password'}), 401
    else:
        print("INVALID FORM DATA:", form.errors)  # Debug statement
        return jsonify({'error': 'Invalid username or password'}), 401


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

        print("Vehicle ID: ", vehicleId)
        print("User ID: ", userId)
        if vehicle is None:
            return jsonify({'error': f'Vehicle with ID {vehicleId} not found'}), 404

        # Add the user to the queued users list
        vehicle.add_queued_user(userId)
        db.session.commit()

        return jsonify({'message': f'User {userId} added to the queued users list for vehicle {vehicleId}'}), 200
    except Exception as e:
        print(f"Error occurred while processing vehicle with ID {vehicleId}: {e}")
        # Handle any unexpected errors
        return jsonify({'error': f'An error occurred: {str(e)}'}), 500


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
def add_location():
    data = request.get_json()
    locationId = generate_location_id()
    userId = data.get('userId')
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
    return jsonify({'message': 'User added successfully'})


@app.route('/update_authorized/<int:userId>', methods=['PUT'])
def update_authorized(userId):
    user = User.query.get(userId)
    user.authorized = True
    db.session.commit()
    return jsonify({'message': 'User authorized successfully'})


@app.route('/ready_driver/<int:userId>', methods=['PUT'])
def ready_driver(userId):
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


@app.route('/get_incoming_passengers/<int:userId>', methods=['GET'])
def get_incoming_passengers(userId):
    user = User.query.get(userId)
    vehicle = Vehicle.query.filter_by(userId=user.userId).first()
    if vehicle:
        queued_users_count = len(json.loads(vehicle.queuedUsers))
        # print("Queued Users Count: ", queued_users_count)
        # return jsonify({'queuedUsersCount': queued_users_count})
        return str(queued_users_count)
    else:
        return jsonify({'error': 'Vehicle not found'})


@app.route('/get_locations', methods=['GET'])
def get_locations():
    # Subquery to get the latest timestamp for each user with user type 2
    latest_timestamps = db.session.query(Location.userId, func.max(Location.timestamp).label('max_timestamp')) \
        .join(User, Location.userId == User.userId) \
        .join(Vehicle, Vehicle.userId == User.userId) \
        .filter(User.userType == 2) \
        .filter(Vehicle.isAvailable == True) \
        .group_by(Location.userId) \
        .subquery()

    # Query to get the latest location for each user with user type 2 and isAvailable vehicles
    latest_locations = db.session.query(Location) \
        .join(latest_timestamps,
              (Location.userId == latest_timestamps.c.userId) &
              (Location.timestamp == latest_timestamps.c.max_timestamp)) \
        .all()

    # Construct JSON response
    response_data = [{'locationId': loc.locationId,
                      'userId': loc.userId,
                      'latitude': loc.latitude,
                      'longitude': loc.longitude} for loc in latest_locations]

    return jsonify(response_data)


@app.route('/get_available_forestry_drivers', methods=['GET'])
def get_available_forestry_drivers():
    drivers = db.session.query(User, Vehicle).join(Vehicle, User.userId == Vehicle.userId).filter(
        User.userType == 2,
        User.authorized.is_(True),
        Vehicle.route == 'Forestry',
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
        'queuedUsers': vehicle.queuedUsers if isinstance(vehicle.queuedUsers, list) else []
    } for user, vehicle in drivers]

    print(drivers_dict)

    return jsonify(drivers_dict)


@app.route('/get_available_rural_drivers', methods=['GET'])
def get_available_rural_drivers():
    drivers = db.session.query(User, Vehicle).join(Vehicle, User.userId == Vehicle.userId).filter(
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
        'queuedUsers': vehicle.queuedUsers if isinstance(vehicle.queuedUsers, list) else []
    } for user, vehicle in drivers]

    print(drivers_dict)

    return jsonify(drivers_dict)


@app.route('/get_auth_drivers', methods=['GET'])
def get_auth_drivers():
    drivers = db.session.query(User, Vehicle).join(Vehicle, User.userId == Vehicle.userId).filter(
        User.userType == 2,
        User.authorized.is_(True)).all()

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

    print(drivers_dict)

    return jsonify(drivers_dict)


#
#
@app.route('/get_pending_drivers', methods=['GET'])
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

    print(drivers_dict)

    return jsonify(drivers_dict)


@app.route('/admin_delete_user/<int:userId>', methods=['DELETE'])
def admin_delete_user(userId):
    try:
        # Step 1: Delete associated vehicle records
        Vehicle.query.filter_by(userId=userId).delete()
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


if __name__ == '__main__':
    print("Flask Server")
    app.run(host='0.0.0.0', port=5000, debug=True)
