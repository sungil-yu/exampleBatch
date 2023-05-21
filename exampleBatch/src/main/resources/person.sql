create table PERSON(
    id bigint primary key auto_increment,
    name varchar(255) not null,
    age int not null,
    address varchar(255) not null
);

insert into PERSON(name, age, address) values('홍길동', 30, '서울');
insert into PERSON(name, age, address) values('김철수', 40, '부산');
insert into PERSON(name, age, address) values('김영희', 50, '대구');
insert into PERSON(name, age, address) values('김영수', 60, '광주');
insert into PERSON(name, age, address) values('이경원', 20, '인천');

