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
from wtforms import StringField, PasswordField, SubmitField, IntegerField
from wtforms.validators import InputRequired, Length, ValidationError, Email
import uuid
import time

app = Flask(__name__)
api = Api(app)
CORS(app)  # Enable CORS for all routes

# Configure your MySQL database URI
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://root:@localhost/travelbetter-dev'
app.config['SECRET_KEY'] = 'thisisasecretkey'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['WTF_CSRF_ENABLED'] = False

db = SQLAlchemy(app)
login_manager = LoginManager(app)
bcrypt = Bcrypt(app)

# Define the User model
class User(db.Model):
    userId = db.Column(db.Integer, primary_key=True)
    fullname = db.Column(db.String(255))
    email = db.Column(db.String(255), unique=True, nullable=False)
    password = db.Column(db.String(255), nullable=False)
    userType = db.Column(db.Integer)
    locationId = db.Column(db.Integer)
    isActive = db.Column(db.Boolean, default=True)

    def is_active(self):
        return self.is_active
    def get_id(self):
        return str(self.userId)  # Convert to string if necessary

def load_user(userId):
    # Load a user from the database using the user ID
    return User.query.get(int(userId))

# Define the Location model
class Location(db.Model):
    locationId = db.Column(db.Integer, primary_key=True)
    userId = db.Column(db.Integer)
    latitude = db.Column(db.Float)
    longitude = db.Column(db.Float)
    timestamp = db.Column(DateTime, default=func.now())

# Test database connection
try:
    with app.app_context():
        db.session.execute(text("SELECT 1"))
        print("Connected to the database successfully!")
except Exception as e:
    print("Failed to connect to the database. Error:", str(e))

# Fetch data using SQLAlchemy
def fetch_data_from_sqlalchemy():
    with app.app_context():
        try:
            data = User.query.all()
            return [{'userId': user.userId, 'fullname': user.fullname, 'email': user.email, 'password': user.password} for user in data]
        except Exception as e:
            return {'error': str(e)}

def generate_new_user_id():
    # Add your logic to generate a new unique user ID here
    # For example, you can query the database for the maximum ID and add 1
    max_id = db.session.query(func.max(User.userId)).scalar()
    return max_id + 1 if max_id is not None else 1

def generate_location_id():
    timestamp = int(time.time() * 1000)
    random_part = uuid.uuid4().int & (2**63 - 1)
    return timestamp + random_part

@login_manager.user_loader
def loader_user(userId):
    return Users.query.get(userId)

class RegisterForm(FlaskForm):
    email = StringField('Email', validators=[InputRequired(), Email(), Length(min=4, max=120)], render_kw={"placeholder": "Email"})
    password = PasswordField(validators=[
                             InputRequired(), Length(min=8, max=20)], render_kw={"placeholder": "Password"})
    fullname = StringField('Full Name', validators=[InputRequired(), Length(min=2, max=255)], render_kw={"placeholder": "Full Name"})
    userType = IntegerField('User Type')

    submit = SubmitField('Register')

    def validate_email(self, email):
        existing_user_email = User.query.filter_by(
            email=email.data).first()
        if existing_user_email:
            raise ValidationError(
                'That email already exists. Please choose a different one.')
@app.route('/register', methods=['POST'])
def register():
    form = RegisterForm()
    print("Form data:", form.email.data, form.fullname.data, form.password.data)
    if form.validate_on_submit():
        userId = generate_new_user_id()
        locationId = generate_location_id()
        hashed_password = bcrypt.generate_password_hash(form.password.data)
        new_user = User(userId=userId, fullname=form.fullname.data, email=form.email.data, password=hashed_password,userType=form.userType.data,locationId=locationId)
        db.session.add(new_user)
        db.session.commit()
        return jsonify({'message': 'User registered successfully'})
    else:
        print(form.errors)
        return jsonify({'error': 'Invalid registration data'}), 400



class LoginForm(FlaskForm):
    email = StringField('Email', validators=[InputRequired(), Email(), Length(min=4, max=120)], render_kw={"placeholder": "Email"})
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
# add a User
@app.route('/add_user', methods=['POST'])
def add_user():
    print("ADD USER")
    data = request.get_json()

    # Extract attributes from the request data
    userId = data.get('userId')
    fullname = data.get('fullname')
    userType = data.get('userType')
    locationId = data.get('locationId')

    # Create a new item with multiple attributes
    new_user = User(userId=userId, fullname=fullname, userType=userType, locationId=locationId)

    db.session.add(new_user)
    db.session.commit()
    return jsonify({'message': 'User added successfully'})


# add a location to user
@app.route('/add_location', methods=['POST'])
def add_location():
    print("ADD LOCATION")
    data = request.get_json()

    # Extract attributes from the request data
    locationId = data.get('locationId')
    userId = data.get('userId')
    latitude = data.get('latitude')
    longitude = data.get('longitude')
    timestamp = datetime.now()

    # Create a new item with multiple attributes
    new_location = Location(locationId=locationId, userId=userId, latitude=latitude, longitude=longitude, timestamp=timestamp)

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
    print("FLASK SERVER")
    # Run the app
    app.run(host='0.0.0.0', port=5000, debug=True)
