from flask import jsonify, request
from app import app, db, bcrypt
from flask_bcrypt import Bcrypt
from flask_login import UserMixin, login_user, LoginManager, login_required, logout_user, current_user
from models import *
from forms import *
import time
import uuid


def generate_new_user_id():
    max_id = db.session.query(func.max(User.userId)).scalar()
    return max_id + 1 if max_id is not None else 1


def generate_location_id():
    timestamp = int(time.time() * 1000)
    random_part = uuid.uuid4().int & (2 ** 63 - 1)
    return timestamp + random_part


@app.route('/register', methods=['POST'])
def register():
    from models import User  # Import User model locally inside the function to avoid circular import
    from forms import RegisterForm
    form = RegisterForm()
    if form.validate_on_submit():
        print(form.contactNumber.data)
        userId = generate_new_user_id()
        locationId = generate_location_id()
        hashed_password = bcrypt.generate_password_hash(form.password.data)
        new_user = User(userId=userId, firstName=form.firstName.data, lastName=form.lastName.data,
                        email=form.email.data, contactNumber=form.contactNumber.data, password=hashed_password,
                        rating=form.rating.data, userType=form.userType.data, isActive=form.isActive.data)
        db.session.add(new_user)
        db.session.commit()
        return jsonify({'message': 'User registered successfully'})
    else:
        print(form.errors)
        return jsonify({'error': 'Invalid registration data'}), 400


@app.route('/signin', methods=['POST'])
def sign_in():
    from models import User  # Import User model locally inside the function to avoid circular import
    from forms import LoginForm
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