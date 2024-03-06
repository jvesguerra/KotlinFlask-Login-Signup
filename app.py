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
from flask_login import LoginManager

app = Flask(__name__)
api = Api(app)
CORS(app)  # Enable CORS for all routes

# Configure your MySQL database URI
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+mysqlconnector://root:@localhost/travelbetter-dev'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)
login_manager = LoginManager(app)

# Define the User model
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    fullname = db.Column(db.String(255))
    email = db.Column(db.String(255), unique=True, nullable=False)
    password = db.Column(db.String(255), nullable=False)
    type = db.Column(db.Integer)
    locationId = db.Column(db.Integer)


# Define the Location model
class Location(db.Model):
    locationId = db.Column(db.Integer, primary_key=True)
    id = db.Column(db.Integer)
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
            return [{'id': user.id, 'fullname': user.fullname, 'email': user.email, 'password': user.password} for user in data]
        except Exception as e:
            return {'error': str(e)}


# Endpoint to fetch data
class DataResource(Resource):
    def get(self):
        data = fetch_data_from_sqlalchemy()
        return jsonify(data)


api.add_resource(DataResource, '/data')


@app.route('/hello', methods=['GET'])
def hello():
    return jsonify(message="Hello from Flask!")


# add a User
@app.route('/add_user', methods=['POST'])
def add_user():
    print("ADD USER")
    data = request.get_json()

    # Extract attributes from the request data
    id = data.get('id')
    fullname = data.get('fullname')
    type = data.get('type')
    locationId = data.get('locationId')

    # Create a new item with multiple attributes
    new_user = User(id=id, fullname=fullname, type=type, locationId=locationId)

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
    id = data.get('id')
    latitude = data.get('latitude')
    longitude = data.get('longitude')
    timestamp = datetime.now()

    # Create a new item with multiple attributes
    new_location = Location(locationId=locationId, id=id, latitude=latitude, longitude=longitude, timestamp=timestamp)

    db.session.add(new_location)
    db.session.commit()
    return jsonify({'message': 'User added successfully'})


@app.route('/signup', methods=['POST'])
def sign_up():
    users = fetch_data_from_sqlalchemy()

    data = request.get_json()
    username = data.get('username')
    password = data.get('password')

    if username and password:
        users[username] = password
        return jsonify({'message': 'User registered successfully'})
    else:
        return jsonify({'error': 'Invalid username or password'}), 400


@app.route('/signin', methods=['POST'])
def sign_in():
    users = fetch_data_from_sqlalchemy()

    data = request.get_json()
    email = data.get('email')
    password = data.get('password')

    user = User.query.filter_by(email=email, password=password).first()

    if user:
        print("LOGGED IN")
        #login_user(user)
        return jsonify({'message': 'User logged in successfully'})
    else:
        print("FAILED TO LOG IN")
        return jsonify({'error': 'Invalid username or password'}), 401


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
