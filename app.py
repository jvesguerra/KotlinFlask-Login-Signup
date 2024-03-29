from flask import Flask, jsonify, request
from sqlalchemy import text, DateTime, func
from sqlalchemy.orm import Mapped
from sqlalchemy.testing.schema import mapped_column
from flask_cors import CORS
from flask_restful import Api, Resource
from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin, login_user, LoginManager, login_required, logout_user, current_user
from flask_bcrypt import Bcrypt
import mysql.connector

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

from routes import *
from models import *
from forms import *


def load_user(userId):
    return Users.query.get(userId)


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


if __name__ == '__main__':
    print("Flask Server")
    app.run(host='0.0.0.0', port=5000, debug=True)
