# from app2 import db
# from sqlalchemy import DateTime, func
# from pydantic import BaseModel
# import json
# from flask_login import UserMixin
#
#
# class User(db.Model):
#     userId = db.Column(db.Integer, primary_key=True)
#     firstName = db.Column(db.String(255))
#     lastName = db.Column(db.String(255))
#     email = db.Column(db.String(255), unique=True, nullable=False)
#     contactNumber = db.Column(db.String(255))
#     password = db.Column(db.String(255), nullable=False)
#
#     rating = db.Column(db.Integer)
#     userType = db.Column(db.Integer)  # 0 = admin, 1 = driver, 2 = student
#     isActive = db.Column(db.Boolean, default=False)
#     authorized = db.Column(db.Boolean, default=False)
#
#     def is_active(self):
#         return self.is_active
#
#     def get_id(self):
#         return str(self.userId)
#
#
# class Location(db.Model):
#     locationId = db.Column(db.Integer, primary_key=True)
#     userId = db.Column(db.Integer)
#     latitude = db.Column(db.Float)
#     longitude = db.Column(db.Float)
#     timestamp = db.Column(DateTime, default=func.now())
#
#
# class Vehicle(db.Model):
#     vehicleId = db.Column(db.Integer, primary_key=True)
#     userId = db.Column(db.Integer)
#     plateNumber = db.Column(db.String(255))
#     route = db.Column(db.String(255))
#     isAvailable = db.Column(db.Boolean, default=False)
#     hasDeparted = db.Column(db.Boolean, default=False)
#     isFull = db.Column(db.Boolean, default=False)
#     queuedUsers = db.Column(db.String(255), default="[]")  # Store as JSON string
#
#     def add_queued_user(self, user_id):
#         queued_users = json.loads(self.queuedUsers)
#         queued_users.append(user_id)
#         self.queuedUsers = json.dumps(queued_users)
#
#     def remove_queued_user(self, user_id):
#         queued_users = json.loads(self.queuedUsers)
#         if user_id in queued_users:
#             queued_users.remove(user_id)
#             self.queuedUsers = json.dumps(queued_users)
