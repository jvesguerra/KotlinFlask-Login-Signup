from app import db
from sqlalchemy import DateTime, func
from pydantic import BaseModel


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
