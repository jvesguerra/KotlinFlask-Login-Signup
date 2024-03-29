from datetime import datetime
from flask import Flask, jsonify, request
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import text, DateTime, func
from sqlalchemy.orm import Mapped
from sqlalchemy.testing.schema import mapped_column
from flask_restful import Resource, Api
from flask_cors import CORS
import mysql.connector
from flask import Flask, jsonify
from flask_restful import Api, Resource
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin, login_user, LoginManager, login_required, logout_user, current_user
from flask_bcrypt import Bcrypt
from flask_wtf import FlaskForm, CSRFProtect
from wtforms import StringField, PasswordField, SubmitField, IntegerField, BooleanField
from wtforms.validators import InputRequired, Length, ValidationError, Email
import uuid
import time
import re

app = Flask(__name__)
api = Api(app)
CORS(app)

# Configure your MySQL database URI
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://root:@localhost/travelbetter-dev'
app.config['SECRET_KEY'] = 'thisisasecretkey'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['WTF_CSRF_ENABLED'] = False

db = SQLAlchemy(app)
login_manager = LoginManager(app)
bcrypt = Bcrypt(app)


class User(db.Model):
    userId = db.Column(db.Integer, primary_key=True)
    firstName = db.Column(db.String(255))
    lastName = db.Column(db.String(255))
    email = db.Column(db.String(255), unique=True, nullable=False)
    contactNumber = db.Column(db.String(255))
    password = db.Column(db.String(255), nullable=False)

    rating = db.Column(db.Integer)
    userType = db.Column(db.Integer)  # 0 = admin, 1 = driver, 2 = student
    isActive = db.Column(db.Boolean, default=True)

    def is_active(self):
        return self.is_active

    def get_id(self):
        return str(self.userId)


# Define the Location model
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


def load_user(userId):
    return User.query.get(int(userId))


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
    timestamp = int(time.time() * 1000)
    random_part = uuid.uuid4().int & (2 ** 63 - 1)
    return timestamp + random_part


@login_manager.user_loader
def loader_user(userId):
    return Users.query.get(userId)





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

    submit = SubmitField('Register')

    def validate_email(self,email):
        existing_user_email = User.query.filter_by(
            email=email.data).first()
        if existing_user_email:
            raise ValidationError(
                'That email already exists. Please choose a different one.')
    def validate_contactNumber(self,contactNumber):
        # Adjust the pattern if necessary to match specific formats
        ph_number_pattern = re.compile(r'^(09|\+639|\+63 9)[0-9]{2}-?[0-9]{3}-?[0-9]{4}$')
        if not ph_number_pattern.match(contactNumber.data):
            raise ValidationError('Invalid Philippine contact number format. Please use formats like 09171234567 or +639171234567.')





@app.route('/register', methods=['POST'])
def register():
    form = RegisterForm()
    if form.validate_on_submit():
        print(form.contactNumber.data)
        userId = generate_new_user_id()
        locationId = generate_location_id()
        hashed_password = bcrypt.generate_password_hash(form.password.data)
        new_user = User(userId=userId, firstName=form.firstName.data, lastName=form.lastName.data, email=form.email.data, contactNumber=form.contactNumber.data, password=hashed_password,
                        rating=form.rating.data, userType=form.userType.data, isActive=form.isActive.data)
        db.session.add(new_user)
        db.session.commit()
        return jsonify({'message': 'User registered successfully'})
    else:
        print(form.errors)
        return jsonify({'error': 'Invalid registration data'}), 400


class LoginForm(FlaskForm):
    email = StringField('Email', validators=[InputRequired(), Email(), Length(min=4, max=120)],
                        render_kw={"placeholder": "Email"})
    password = PasswordField(validators=[
        InputRequired(), Length(min=8, max=20)], render_kw={"placeholder": "Password"})
    submit = SubmitField('Login')


@app.route('/signin', methods=['POST'])
def sign_in():
    form = LoginForm()
    print("Form data:", form.email.data, form.password.data)
    if form.validate_on_submit():
        user = User.query.filter_by(email=form.email.data).first()
        if user:
            if bcrypt.check_password_hash(user.password, form.password.data):
                print("LOGGED IN")
                print(user.userType)
                login_user(user)  # Log in the user
                return jsonify({
                    'message': 'User logged in successfully',
                    'user': {
                        'userId': user.userId,
                        'fullname': user.fullname,
                        'email': user.email,
                        'password': user.password,
                        'userType': user.userType,
                        'locationId': user.locationId
                    }
                })
            else:
                print("FAILED TO LOG IN")
                return jsonify({'error': 'Invalid username or password'}), 401
    else:
        return jsonify({'error': 'Invalid username or password'}), 401


@app.route("/logout")
@login_required
def logout():
    logout_user()
    return jsonify({'message': 'Logged out'})


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

    locationId = data.get('locationId')
    userId = data.get('userId')
    latitude = data.get('latitude')
    longitude = data.get('longitude')
    timestamp = datetime.now()

    new_location = Location(locationId=locationId, userId=userId, latitude=latitude, longitude=longitude,
                            timestamp=timestamp)

    db.session.add(new_location)
    db.session.commit()
    return jsonify({'message': 'User added successfully'})


# Endpoint to fetch all locations
# @app.route('/locations', methods=['GET'])
# def get_locations():
#     locations = Location.query.all()
#     location_list = []
#
#     for location in locations:
#         location_data = {
#             'locationId': location.locationId,
#             'id': location.id,
#             'latitude': location.latitude,
#             'longitude': location.longitude,
#             'timestamp': location.timestamp.timestamp()  # Convert datetime to timestamp in seconds
#         }
#         location_list.append(location_data)
#
#     print(location_list)
#     return jsonify({'locations': location_list})
@app.route('/get_locations', methods=['GET'])
def get_locations():
    locations = Location.query.all()
    return jsonify(
        [{'locationId': loc.locationId, 'id': loc.id, 'latitude': loc.latitude, 'longitude': loc.longitude} for loc in
         locations])


if __name__ == '__main__':
    print("Flask Server")
    app.run(host='0.0.0.0', port=5000, debug=True)
