from flask_wtf import FlaskForm, CSRFProtect
from wtforms import StringField, PasswordField, SubmitField, IntegerField, BooleanField
from wtforms.validators import InputRequired, Length, ValidationError, Email
import re
from models import *
from flask_cors import CORS, cross_origin


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
