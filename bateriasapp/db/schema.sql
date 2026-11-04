CREATE TABLE productos (
	id int auto_increment primary key,
    nombre varchar(100) not null,
    tipo varchar(20)not null check (tipo IN ('bateria', 'claxon', 'luz')),
    amperaje int,
    cca int,
    tipo_caja varchar(50),
    polaridad varchar(50),
    precio_base decimal(10,2) not null,
    condicion enum('nueva', 'segunda') not null default 'nueva',
    disponible boolean not null default true
);

create table vehiculos_referencia(
	id int auto_increment primary key,
    marca varchar(100) not null,
    modelo varchar(100) not null,
    anio int not null,
    amperaje_recomendado int,
    cca_recomendado int
);

create table clientes (
	id int auto_increment primary key,
    nombre varchar(100) not null,
    cedula varchar(50) not null unique,
    telefono varchar(20) not null,
    direccion varchar(100) not null,
    ciudad varchar(100) not null default 'Bogota'
);

create table domiciliarios (
	id int auto_increment primary key,
    nombre varchar(100) not null,
    telefono varchar(100) not null
);

create table pedidos (
	id int auto_increment primary key,
    cliente_id int not null,
    producto_id int not null,
    metodo_pago VARCHAR(20) NOT NULL CHECK (metodo_pago IN ('efectivo', 'transferencia', 'tarjeta', 'addi')),
    entrega_bateria_usada boolean not null default false,
    precio_final decimal(10,2) not null,
    estado varchar(50) not null default 'cotizado' check (estado in ('cotizado', 'confirmado', 'entregado')) ,
    fecha_creacion datetime not null default current_timestamp,
    foreign key (producto_id) references productos(id),
    foreign key (cliente_id) references clientes(id)
);

create table entregas (
	id int auto_increment primary key,
    pedido_id int not null,
    domiciliario_id int not null,
    fecha_entrega datetime,
    instalada boolean not null default false,
    foreign key (domiciliario_id) references domiciliarios(id),
    foreign key (pedido_id) references pedidos(id)
);


create table facturas (
	id int auto_increment primary key,
    pedido_id int not null unique,
    numero_factura varchar(100) not null unique,
    fecha datetime not null default current_timestamp,
    total decimal(10,2) not null,
    foreign key (pedido_id) references pedidos(id)
);

ALTER TABLE productos MODIFY COLUMN condicion VARCHAR(20) NOT NULL DEFAULT 'nueva' CHECK (condicion IN ('nueva','segunda'));

select * from productos;