from flask import Flask, jsonify, request
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired
from functools import wraps
import os

app = Flask(__name__)

# 1. Configuracion de la Base de Datos (SQLite)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///site.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'llave-de-desarrollo')

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)
serializer = URLSafeTimedSerializer(app.config['SECRET_KEY'])
TOKEN_MAX_AGE = 3600  # el token expira en 1 hora


# 2. Modelos (las tablas en la BD)
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(20), unique=True, nullable=False)
    password = db.Column(db.String(60), nullable=False)  # aqui guardamos el hash

    def __repr__(self):
        return f"User('{self.username}')"


class Tarea(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    titulo = db.Column(db.String(100), nullable=False)
    descripcion = db.Column(db.String(300))
    prioridad = db.Column(db.String(10), nullable=False)
    completada = db.Column(db.Boolean, nullable=False, default=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)


# 3. Seguridad: decorador que protege rutas
def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.headers.get('Authorization', '')
        if not auth.startswith('Bearer '):
            return jsonify({"message": "Token faltante"}), 401
        token = auth.split(' ', 1)[1]
        try:
            data = serializer.loads(token, max_age=TOKEN_MAX_AGE)
        except SignatureExpired:
            return jsonify({"message": "Token expirado"}), 401
        except BadSignature:
            return jsonify({"message": "Token invalido"}), 401
        user = User.query.filter_by(id=data.get('user_id')).first()
        if not user:
            return jsonify({"message": "Usuario no encontrado"}), 401
        return f(user, *args, **kwargs)
    return decorated


def tarea_a_dict(t):
    return {
        "id": t.id,
        "titulo": t.titulo,
        "descripcion": t.descripcion,
        "prioridad": t.prioridad,
        "completada": t.completada,
        "user_id": t.user_id
    }


# 4. Rutas de autenticacion
@app.route('/')
def hello():
    return jsonify({"message": "API Funcionando"})


@app.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')

    if not username or not password:
        return jsonify({"message": "Usuario y contrasenia son obligatorios"}), 400

    if User.query.filter_by(username=username).first():
        return jsonify({"message": "El usuario ya existe"}), 400

    hashed_password = bcrypt.generate_password_hash(password).decode('utf-8')
    new_user = User(username=username, password=hashed_password)
    db.session.add(new_user)
    db.session.commit()
    return jsonify({"message": "Usuario creado exitosamente"}), 201


@app.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')

    user = User.query.filter_by(username=username).first()

    if user and bcrypt.check_password_hash(user.password, password):
        token = serializer.dumps({"user_id": user.id})
        return jsonify({
            "status": "success",
            "message": "Login exitoso",
            "token": token,
            "user_id": user.id,
            "username": user.username
        }), 200
    else:
        return jsonify({"status": "error", "message": "Credenciales invalidas"}), 401


# 5. CRUD de tareas (rutas protegidas por token)
@app.route('/tareas', methods=['POST'])
@token_required
def crear_tarea(usuario_actual):
    data = request.get_json()
    if not data or not data.get('titulo'):
        return jsonify({"message": "El titulo es obligatorio"}), 400

    nueva = Tarea(
        titulo=data.get('titulo'),
        descripcion=data.get('descripcion'),
        prioridad=data.get('prioridad', 'media'),
        user_id=usuario_actual.id
    )
    db.session.add(nueva)
    db.session.commit()
    return jsonify(tarea_a_dict(nueva)), 201


@app.route('/tareas', methods=['GET'])
@token_required
def listar_tareas(usuario_actual):
    tareas = Tarea.query.filter_by(user_id=usuario_actual.id).all()
    return jsonify([tarea_a_dict(t) for t in tareas]), 200


@app.route('/tareas/<int:tarea_id>', methods=['PUT'])
@token_required
def actualizar_tarea(usuario_actual, tarea_id):
    tarea = Tarea.query.filter_by(id=tarea_id, user_id=usuario_actual.id).first()
    if not tarea:
        return jsonify({"message": "Tarea no encontrada"}), 404

    data = request.get_json() or {}
    tarea.titulo = data.get('titulo', tarea.titulo)
    tarea.descripcion = data.get('descripcion', tarea.descripcion)
    tarea.prioridad = data.get('prioridad', tarea.prioridad)
    tarea.completada = data.get('completada', tarea.completada)
    db.session.commit()
    return jsonify(tarea_a_dict(tarea)), 200


@app.route('/tareas/<int:tarea_id>', methods=['DELETE'])
@token_required
def borrar_tarea(usuario_actual, tarea_id):
    tarea = Tarea.query.filter_by(id=tarea_id, user_id=usuario_actual.id).first()
    if not tarea:
        return jsonify({"message": "Tarea no encontrada"}), 404

    db.session.delete(tarea)
    db.session.commit()
    return jsonify({"message": "Tarea eliminada"}), 200


if __name__ == '__main__':
    with app.app_context():
        db.create_all()
    app.run(host='0.0.0.0', port=5000, debug=True)