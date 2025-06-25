-- If migrating from scratch
-- Most of the time temp tables are already present, from previous migration, in the database dump provided by EA. Drop these tables as new ones will be created based on latest mambu dump
-- Manually drop all tmp_xxx tables
truncate m_client restart identity cascade;
truncate m_loan restart identity cascade;
truncate m_portfolio_command_source  restart identity cascade;
truncate m_document restart identity cascade;

CREATE EXTENSION IF NOT EXISTS unaccent;

---- MOVE DATA TO LOCAL MACHINE/MIGRATION TENANT from mambu--------------------
-- I am using dbeaver. run below queries in mambu and export the result in fineract. Each query result will be exported into a new table. Table name is mentioned on top of each query

-- tmp_cliente_migrar
select c.*, a .city, a.country, a.line1, a.REGION, i.documentid, i.DOCUMENTTYPE
from client c 
join address a on a.parentkey = c.ENCODEDKEY
join identificationdocument i on i.CLIENTKEY = c.ENCODEDKEY
where state = 'ACTIVE';

-- tmp_loanaccount
select * from loanaccount l where accountstate = 'ACTIVE' OR (accountstate = 'ACTIVE_IN_ARREARS' and accountsubstate IS NULL);

-- tmp_loantransaction
select t.* from loantransaction t
join loanaccount l on l.ENCODEDKEY = t.PARENTACCOUNTKEY
where l.ACCOUNTSTATE in ('ACTIVE', 'ACTIVE_IN_ARREARS')
and t.`TYPE` in ('REPAYMENT', 'DISBURSMENT')
and t.REVERSALTRANSACTIONKEY is null;

-- tmp_disbursementdetails
select d.* from disbursementdetails d 
join loanaccount l on l.DISBURSEMENTDETAILSKEY = d.ENCODEDKEY
where l.ACCOUNTSTATE in ('ACTIVE', 'ACTIVE_IN_ARREARS');

-- tmp_loan_repayment_schedule
select r.* from repayment r
join loanaccount l on l.ENCODEDKEY  = r.PARENTACCOUNTKEY 
where l.ACCOUNTSTATE in ('ACTIVE', 'ACTIVE_IN_ARREARS')
order by r.PARENTACCOUNTKEY, r.DUEDATE;

-- tmp_loan_product
select * from loanproduct;

-- custom fields
-- tmp_customfieldvalue
select l.ENCODEDKEY , l.id loanid, c.ID field_id, c.NAME field_name, cfv.value field_value, cfv.AMOUNT field_amount
from customfieldvalue cfv
join customfield c on c.ENCODEDKEY = cfv.CUSTOMFIELDKEY
join loanaccount l on l.ENCODEDKEY = cfv.PARENTKEY
where 
l.ACCOUNTSTATE in ('ACTIVE', 'ACTIVE_IN_ARREARS')
-- cfv.PARENTKEY = '8a4441fd9148030d019148d5289c291b'
order by l.encodedkey, c.id, c.name;


-- Create table to store mambu charge details against loans
-- tmp_loan_charges
select distinct l3.ENCODEDKEY loankey, pf.NAME, pf.PERCENTAGEAMOUNT fee_percentage, pf.FEEAPPLICATION, pf.AMOUNTCALCULATIONMETHOD, pf.NONTAXABLEFEE, pf.ENCODEDKEY
from predefinedfeeamount p 
join predefinedfee pf on pf.ENCODEDKEY = p.FEE_ENCODEDKEY_OID 
join loanproduct l on l.ENCODEDKEY = pf.LOANFEES_ENCODEDKEY_OWN
join loantransaction l2 on l2.ENCODEDKEY = p.loanpredefinedfeeamounts_encodedkey_own
join loanaccount l3 on l3.ENCODEDKEY = l2.PARENTACCOUNTKEY
where 
--  l3.ENCODEDKEY = '8a44201b930cb923019312df0b3050ac' and 
l2.REVERSALTRANSACTIONKEY is null and l2.type in ('FEE')
and pf.AMOUNTCALCULATIONMETHOD  !='FLAT'
-- group by l3.ENCODEDKEY having count(pf.name) > 1
order by l3.ENCODEDKEY , pf.NAME;


-- flat loan charges
-- tmp_flat_loan_charges
select l3.id product_id ,l2.ENCODEDKEY loankey, l2.id loan_external_id, fee.NAME fee_name, feeamount.AMOUNT, rd.FEEDUE, rd.FEEPAID , r.ENCODEDKEY installmentkey 
from repaymentfeedetails rd
join repayment r on r.ENCODEDKEY = rd.REPAYMENTFEEDETAILS_ENCODEDKEY_OWN
join loantransaction l on l.ENCODEDKEY = rd.LOANTRANSACTIONKEY
join predefinedfeeamount feeamount on feeamount.loanpredefinedfeeamounts_encodedkey_own = l.ENCODEDKEY
join predefinedfee fee on fee.ENCODEDKEY = feeamount.FEE_ENCODEDKEY_OID
join loanaccount l2 on l2.ENCODEDKEY = l.PARENTACCOUNTKEY
join loanproduct l3 on l3.encodedkey = l2.PRODUCTTYPEKEY
where 
fee.AMOUNTCALCULATIONMETHOD = 'FLAT'
and l.REVERSALTRANSACTIONKEY is null and l.type in ('FEE')
and l2.ACCOUNTSTATE in ('ACTIVE','ACTIVE_IN_ARREARS')
-- and fee.name like 'Comisión MiPyme%'
-- and l3.id not in ('Nano_cred_sem','Credito_Rotativo','Nano_cred_dia')
-- and l3.id = 'Microcredito_B'
order by l2.ENCODEDKEY, r.DUEDATE;

--select * from tmp_flat_loan_charges tflc where fee_name not like '%MiPyme%';


-- installment count setting for multidisbursal loans
-- tmp_principalpaymentaccountsettings
select * from principalpaymentaccountsettings

-------------------------------------

CREATE INDEX tmp_loanaccount_id_idx ON public.tmp_loanaccount ("ID");

-- Create bridge tables

CREATE TABLE public.tmp_loanproduct_mapping (
	mifos_product_name varchar NULL,
	ea_product_id varchar null,
	ea_product_key varchar null
);

-- Verify fineract product names first as they are constantly changing the produt names specially for multidisbursal loans
insert into tmp_loanproduct_mapping values('Credito Comercial PN','Credito_Com_PN','8a443321877f667c0187813a08e6175e');
insert into tmp_loanproduct_mapping values('Crédito Libre Inversión','cred_lib_inv','8a44533392968791019297110513633a');
insert into tmp_loanproduct_mapping values('Credito Comercial PJ','Credito_Com_PJ','8a443aa293ad6ed30193ad8d8e462ddc');
insert into tmp_loanproduct_mapping values('Microcredito M','MicrocreditoM','8a4442a98212792801821331924c521d');
insert into tmp_loanproduct_mapping values('Microcredito','Microcredito','8a44488480ff61ab018105dcd82344ca');
insert into tmp_loanproduct_mapping values('Crédito rotativo','Credito_Rotativo','8a4426a98a4d0efb018a4d7c210b5b66');
insert into tmp_loanproduct_mapping values('Nano Credito Semanal','Nano_cred_sem','8a4440af8f1a7b2e018f1c6bd0c64b14');
insert into tmp_loanproduct_mapping values('Microcredito B','Microcredito_B','8a4431d691336a4c019134e082030c38');
insert into tmp_loanproduct_mapping values('Bajo Monto','Bajo_monto','8a44504485cb20640185cbecc09457bc');
insert into tmp_loanproduct_mapping values('Nano Credito Diario','Nano_cred_dia','8a4451df8f1c428e018f1cd4c19f02f0');

-- Update code values mappings

UPDATE public.m_code_value
	SET code_description='CC'
	WHERE code_value = 'Cédula de ciudadanía'
	and code_id = (select id from m_code where code_name ='Customer Identifier');

-- Delete duplicate code values for Ciudad
DELETE FROM m_code_value
WHERE code_id = (select id from m_code where code_name='Ciudad') and id IN
    (SELECT id
    FROM 
        (SELECT id,
         ROW_NUMBER() OVER( PARTITION BY code_value ORDER BY  id ) AS row_num
        FROM m_code_value ) t
        WHERE t.row_num > 1 );

update tmp_cliente_migrar set city = 'ÁBREGO' where lower(city) = lower('Abrego');
update tmp_cliente_migrar set city = 'ACACÍAS' where lower(city) = lower('Acacias');
update tmp_cliente_migrar set city = 'ACACÍAS' where lower(city) = lower('ACACIAS');
update tmp_cliente_migrar set city = 'AMAGA' where lower(city) = lower('Amagá');
update tmp_cliente_migrar set city = 'APARTADO' where lower(city) = lower('Apartadó');
update tmp_cliente_migrar set city = 'ARGELIA' where lower(city) = lower('Argelia (Cauca)');
update tmp_cliente_migrar set city = 'ARMENIA' where lower(city) = lower('Armenia (Quindío)');
update tmp_cliente_migrar set city = 'BARBOSA' where lower(city) = lower('Barbosa (Antioquia)');
update tmp_cliente_migrar set city = 'BARBOSA' where lower(city) = lower('Barbosa (Santander)');

update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('BOGOTA D.C');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('BOGOTA D.C.');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('Bogota, D.C.');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('BOGOTA, D.C.');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('Bogotá');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('bogota');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('Bogotá D.C');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('Bogotá, D.C');
update tmp_cliente_migrar set city = 'BOGOTÁ D.C.' where lower(city) = lower('Bogotá, D.C.');
update tmp_cliente_migrar set city = 'BOJACÁ' where lower(city) = lower('Bojaca');
update tmp_cliente_migrar set city = 'CAJICÁ' where lower(city) = lower('Cajica');
update tmp_cliente_migrar set city = 'CALDAS' where lower(city) = lower('Caldas (Antioquia)');
update tmp_cliente_migrar set city = 'CANDELARIA' where lower(city) = lower('Candelaria (Valle Del Cauca)');
update tmp_cliente_migrar set city = 'CHÍA' where lower(city) = lower('Chia');
update tmp_cliente_migrar set city = 'CHÍA' where lower(city) = lower('chia');
update tmp_cliente_migrar set city = 'CHIGORODO' where lower(city) = lower('Chigorodó');
update tmp_cliente_migrar set city = 'CHIQUINQUIRÁ' where lower(city) = lower('Chiquinquira');
update tmp_cliente_migrar set city = 'CHOCONTÁ' where lower(city) = lower('Choconta');
update tmp_cliente_migrar set city = 'CIÉNAGA' where lower(city) = lower('Cienaga');
update tmp_cliente_migrar set city = 'CIÉNAGA DE ORO' where lower(city) = lower('Cienaga de Oro');
update tmp_cliente_migrar set city = 'CONCORDIA' where lower(city) = lower('Concordia (Antioquia)');
update tmp_cliente_migrar set city = 'CUCUNUBÁ' where lower(city) = lower('Cucunuba');
update tmp_cliente_migrar set city = 'CÚCUTA' where lower(city) = lower('Cucuta');
update tmp_cliente_migrar set city = 'CURRILLO' where lower(city) = lower('Curillo');
update tmp_cliente_migrar set city = 'COMBITA' where lower(city) = lower('Cómbita');
update tmp_cliente_migrar set city = 'DISTRACCION' where lower(city) = lower('Distracción');
update tmp_cliente_migrar set city = 'DOS QUEBRADAS' where lower(city) = lower('Dosquebradas');
update tmp_cliente_migrar set city = 'DOS QUEBRADAS' where lower(city) = lower('DOSQUEBRADAS');
update tmp_cliente_migrar set city = 'EL RETÉN' where lower(city) = lower('El Reten');
update tmp_cliente_migrar set city = 'FACATATIVÁ' where lower(city) = lower('Facatativa');
update tmp_cliente_migrar set city = 'FLORENCIA' where lower(city) = lower('Florencia (Caquetá)');
update tmp_cliente_migrar set city = 'FLORENCIA' where lower(city) = lower('Florencia (Cauca)');
update tmp_cliente_migrar set city = 'FUSAGASUGÁ' where lower(city) = lower('Fusagasuga');
update tmp_cliente_migrar set city = 'GIRÓN' where lower(city) = lower('GIRON');
update tmp_cliente_migrar set city = 'GIRÓN' where lower(city) = lower('Giron');
update tmp_cliente_migrar set city = 'GRANADA' where lower(city) = lower('GRANADA (META)');
update tmp_cliente_migrar set city = 'GUACHETÁ' where lower(city) = lower('Guacheta');
update tmp_cliente_migrar set city = '' where lower(city) = lower('GUADALAJARA DE BUGA');
update tmp_cliente_migrar set city = 'IBAGUÉ' where lower(city) = lower('IBAGUE');
update tmp_cliente_migrar set city = 'IBAGUÉ' where lower(city) = lower('Ibague');
update tmp_cliente_migrar set city = 'ITAGÜI' where lower(city) = lower('Itagui');
update tmp_cliente_migrar set city = 'JAMUNDÍ' where lower(city) = lower('Jamundi');
update tmp_cliente_migrar set city = 'LA UNION' where lower(city) = lower('La Unión (Antioquia)');
update tmp_cliente_migrar set city = 'LEBRIJA' where lower(city) = lower('Lebríja');
update tmp_cliente_migrar set city = 'LEJANÍAS' where lower(city) = lower('Lejanias');
update tmp_cliente_migrar set city = 'MÁLAGA' where lower(city) = lower('Malaga');
update tmp_cliente_migrar set city = 'MARÍA LA BAJA' where lower(city) = lower('Maria la Baja');
update tmp_cliente_migrar set city = 'MEDELLIN' where lower(city) = lower('MEDELLÍN');
update tmp_cliente_migrar set city = 'MEDELLIN' where lower(city) = lower('Medellín');
update tmp_cliente_migrar set city = 'MESETAS' where lower(city) = lower('Mesitas del Colegio');
update tmp_cliente_migrar set city = 'MOMPÓS' where lower(city) = lower('Mompos');
update tmp_cliente_migrar set city = 'MONTERÍA' where lower(city) = lower('Monteria');
update tmp_cliente_migrar set city = 'MOSQUERA' where lower(city) = lower('Mosquera (Cundinamarca)');
update tmp_cliente_migrar set city = 'PALESTINA' where lower(city) = lower('Palestina (Caldas)');
update tmp_cliente_migrar set city = 'PIJINO DEL CARMEN' where lower(city) = lower('Pijiño Del Carmen');
update tmp_cliente_migrar set city = 'POPAYÁN' where lower(city) = lower('Popayan');
update tmp_cliente_migrar set city = 'PUERTO BERRIO' where lower(city) = lower('Puerto Berrío');
update tmp_cliente_migrar set city = 'PUERTO COLOMBIA' where lower(city) = lower('Puerto Colombia (Atlántico)');
update tmp_cliente_migrar set city = 'PUERTO SANTANDER' where lower(city) = lower('Puerto Santander (Norte De Santander)');
update tmp_cliente_migrar set city = 'RAMIRIQUÍ' where lower(city) = lower('Ramiriqui');
update tmp_cliente_migrar set city = 'RIONEGRO' where lower(city) = lower('Rionegro (Antioquia)');

update tmp_cliente_migrar set city = 'SAN ANDRÉS DE SOTAVET0' where lower(city) = lower('San Andrés Sotavento');
update tmp_cliente_migrar set city = 'SAN JERONIMO' where lower(city) = lower('San Jerónimo');
update tmp_cliente_migrar set city = 'SAN JERONIMO' where lower(city) = lower('SAN JERÓNIMO');
update tmp_cliente_migrar set city = 'SAN JOSÉ DE LA FRAGUA' where lower(city) = lower('San Jose del Fragua');
update tmp_cliente_migrar set city = 'SAN JOSÉ DE LA FRAGUA' where lower(city) = lower('San José Del Fragua');
update tmp_cliente_migrar set city = 'SAN MARTÍN DE LOBA' where lower(city) = lower('San Martin de Loba');
update tmp_cliente_migrar set city = 'SAN MARTÍN' where lower(city) = lower('San Martín (Cesar)');
update tmp_cliente_migrar set city = 'SANTA MARÍA' where lower(city) = lower('Santa Maria');
update tmp_cliente_migrar set city = 'SANTAFE DE ANTIOQUIA' where lower(city) = lower('Santafé De Antioquia');
update tmp_cliente_migrar set city = 'SANTIAGO' where lower(city) = lower('Santiago De Tolú');
update tmp_cliente_migrar set city = 'SANTO TOMAS' where lower(city) = lower('Santo Tomás');
update tmp_cliente_migrar set city = 'SESQUILÉ' where lower(city) = lower('Sesquile');
update tmp_cliente_migrar set city = 'SIBATÉ' where lower(city) = lower('Sibate');
update tmp_cliente_migrar set city = 'SOATÁ' where lower(city) = lower('Soata');
update tmp_cliente_migrar set city = 'SONSON' where lower(city) = lower('Sonsón');
update tmp_cliente_migrar set city = 'SOPETRAN' where lower(city) = lower('Sopetrán');

update tmp_cliente_migrar set city = 'TOCANCIPÁ' where lower(city) = lower('Tocancipa');
update tmp_cliente_migrar set city = 'TOLÚ' where lower(city) = lower('Tolu Viejo');
update tmp_cliente_migrar set city = 'TOLÚ' where lower(city) = lower('Tolú Viejo');
update tmp_cliente_migrar set city = 'TORIBÍO' where lower(city) = lower('Toribio');
update tmp_cliente_migrar set city = 'TAMESIS' where lower(city) = lower('Támesis');



update tmp_cliente_migrar set city = 'VILLANUEVA' where lower(city) = lower('Villanueva (La Guajira)');
update tmp_cliente_migrar set city = 'VILLANUEVA' where lower(city) = lower('Villanueva (Santander)');
update tmp_cliente_migrar set city = 'YONDO' where lower(city) = lower('Yondó');
update tmp_cliente_migrar set city = 'ZIPAQUIRA' where lower(city) = lower('Zipaquirá');
update tmp_cliente_migrar set city = 'ZIPAQUIRA' where lower(city) = lower('ZIPAQUIRÁ');
update tmp_cliente_migrar set city = 'Nariño' where lower(city) = lower('Nariño (Nariño)');


--- Run Below query to see which cities are missing and execute below queries based on missing cities or create new query if city not in one of below queries
select distinct city from tmp_cliente_migrar tcm where lower(unaccent(trim(tcm.city))) not in (select lower(unaccent(trim(code_value))) from m_code_value where code_id = 41);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Berlín (Santander)', '', 11260, '99775', true, false);
INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'SAN ANDRES DE TUMACO', '', 11270, '99776', true, false);
INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Suba', '', 11280, '99777', true, false);
INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Villa de San Diego de Ubate', '', 11290, '99778', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'CALDAS', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Nariño', '', 11330, '99800', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Arauca', '', 11340, '99810', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'San Andres', '', 11350, '99820', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'La Unión (Valle Del Cauca)', '', 11360, '99830', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'San Pablo (Nariño)', '', 11370, '99840', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'BOLIVAR', '', 11380, '99850', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Cachira', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Calarca', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'San Cristóbal', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Santa Bárbara De Pinto', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Betulia (Santander)', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Santa Rosa (Bolívar)', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Guamal (Magdalena)', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'Sabanalarga (Atlántico)', '', 11320, '99790', true, false);

INSERT INTO m_code_value
(code_id, code_value, code_description, order_position, code_score, is_active, is_mandatory)
VALUES((select id from m_code where code_name='Ciudad'), 'FLORENCIA (CAQUETÁ)', '', 11320, '99790', true, false);



-- Update empty cities
update tmp_cliente_migrar set city = 'CANDELARIA' where city = '';



-- Check duplicate mobile and document ids

-- Check which client is older and mark inactive. Move its loans to latest cleint
select encodedkey,id, state, MOBILEPHONE1 from tmp_cliente_migrar tcm where tcm.mobilephone1 in (select  c.MOBILEPHONE1  from tmp_cliente_migrar c
group by c.MOBILEPHONE1 having count(c.MOBILEPHONE1) > 1);

select * from tmp_loanaccount where "ACCOUNTHOLDERKEY" in (select encodedkey from tmp_cliente_migrar tcm where tcm.mobilephone1 in (select  c.MOBILEPHONE1  from tmp_cliente_migrar c
group by c.MOBILEPHONE1 having count(c.MOBILEPHONE1) > 1));


-- Client Import

insert into m_client (account_no, external_id, status_enum, submittedon_date, activation_date, office_joining_date, office_id, firstname, middlename, lastname, second_lastname, display_name,
						mobile_no, gender_cv_id, date_of_birth, legal_form_enum, email_address, created_on_utc, last_modified_on_utc,  activatedon_userid, created_by, last_modified_by )
select 
	c.ID as account_no,
	c.ID  as external_id, 
	300 as status_enum,
	c.APPROVEDDATE  as submittedon_date,
	c.ACTIVATIONDATE  as activation_date,
	c.ACTIVATIONDATE  as office_joining_date,
	1 as office_id,
	trim(c.FIRSTNAME ) as firstname,
	coalesce(c.MIDDLENAME , null) as middlename,
	c.LASTNAME  as lastname,
	null as second_lastname,
	c.FIRSTNAME  || case when c.MIDDLENAME  is null then ' ' else ' '  || c.MIDDLENAME || ' ' end || c.LASTNAME as display_name,
	c.MOBILEPHONE1  as mobile_no,
	CASE 
		when trim(c.GENDER ) = 'FEMALE' then (select id from m_code_value where code_value='Mujer' and code_id = (select id from m_code where code_name='Gender'))
		when trim(c.GENDER ) = 'MALE' then (select id from m_code_value where code_value='Hombre' and code_id = (select id from m_code where code_name='Gender'))
		ELSE null
	END as gender_cv_id,
	c.BIRTHDATE  as date_of_birth,
	1 as legal_form_enum,
	c.EMAILADDRESS  as email_address,
	current_date as created_on_utc,
	current_date as last_modified_on_utc,
	1 as activatedon_userid, 
	1 as created_by, 
	1 as last_modified_by
	from
	tmp_cliente_migrar c
	where state = 'ACTIVE';

-- Campos client persona import

INSERT INTO campos_cliente_persona
(client_id, "Cedula", "Estado Civil_cd_Estado civil", "Edad", "Estrato", "Ciudad_cd_Ciudad", "Tiene vehiculo propio", "Tipo Vehiculo_cd_Tipo de vehiculo", "Nivel Academico_cd_Nivel academico", 
"Actividad Laboral_cd_Actividad laboral", "Tiempo de actividad laboral", "Media de ingresos", "Nombre empresa", "Direccion", "Telefono", "Cupo solicitado", "Cupo aprobado", "Cupo score", 
"Valor score", "Modelo score", created_at, updated_at, "Cupo otros prestamos", "Customer Identifier_cd_Tipo identificacion" )
select distinct 
	mc.id as client_id,
	c.documentid  as Cedula,
	 0 as "Estado Civil_cd_Estado civil",
	0 as Edad,
	0 as Estrato,
	-- city_value.id as Ciudad_cd_Ciudad,
	-- Below query part for city is temporary as I do not have complete city mappings
	(select city.id from m_code_value city where lower(unaccent(trim(city.code_value))) = lower(unaccent(trim(c.city))) and code_id = (select id from m_code where code_name ='Ciudad') limit 1) as Ciudad_cd_Ciudad,
	false	as "Tiene vehiculo propio",
	 null::integer as "Tipo Vehiculo_cd_Tipo de vehiculo",
	null::integer as "Nivel Academico_cd_Nivel academico",
	null::integer as "Actividad Laboral_cd_Actividad laboral",
	null::integer as "Tiempo de actividad laboral",
	0 as "Media de ingresos",
	null::integer as "Nombre empresa",
	c.line1 as Direccion,
	null::integer as Telefono,
	100000000 as "Cupo solicitado",
	100000000 as "Cupo aprobado",
	null::integer as "Cupo score",
	null::integer as "Valor score",
	null::integer as "Modelo score",
	current_date as created_at,
	current_date as updated_at,
	100000000 as "cupo otros prestamos",
	tipo.id
from 
tmp_cliente_migrar c
join m_client mc on mc.external_id = c.ID::varchar 
  join m_code_value city_value on lower(unaccent(trim(city_value.code_value))) = lower(unaccent(trim(c.city)))
 join m_code city_code on city_code.id = city_value.code_id and city_code.code_name ='Ciudad'
 left join m_code_value tipo on tipo.code_value = c.documenttype  and tipo.code_id = (select id from m_code where code_name ='Customer Identifier')
 where 
  c.STATE = 'ACTIVE'
and mc.id not in (select client_id from campos_cliente_persona);

--If this query above fails, check the code_values vs the document type -- select * from m_code_value mcv where mcv.code_id = (select id from m_code where code_name ='Customer Identifier');



--- Add installment column in source repayment table
ALTER TABLE tmp_loan_repayment_schedule ADD installment INTEGER NULL;

UPDATE 
  tmp_loan_repayment_schedule dest
SET
  installment = src.inst_no
  from (
    select r."PARENTACCOUNTKEY" ,r."ENCODEDKEY" ,"DUEDATE", row_number() over (partition by r."PARENTACCOUNTKEY" order by r."DUEDATE" ) inst_no
from tmp_loan_repayment_schedule r 
join tmp_loanaccount l on l."ENCODEDKEY"  = r."PARENTACCOUNTKEY"
where l."ACCOUNTSTATE"  in ('ACTIVE', 'ACTIVE_IN_ARREARS')
and r."STATE" !='GRACE'
  ) src 
WHERE dest."PARENTACCOUNTKEY" = src."PARENTACCOUNTKEY"
  AND dest."ENCODEDKEY" = src."ENCODEDKEY" ;

-- Add Fineract Penalty Charge to Mambu Product Mapping
CREATE TABLE public.tmp_loanproduct_penalty_charge_mapping (
	ea_product_id varchar null,
	fineract_penalty_charge_name varchar null,
	ea_product_key varchar null
);

insert into tmp_loanproduct_penalty_charge_mapping values('Credito_Com_PN','Mora diaria Credito Comercial PN','8a443321877f667c0187813a08e6175e');
insert into tmp_loanproduct_penalty_charge_mapping values('cred_lib_inv','Mora diaria Crédito Libre Inversión','8a44533392968791019297110513633a');
insert into tmp_loanproduct_penalty_charge_mapping values('Credito_Com_PJ','Mora diaria Credito Comercial PJ','8a443aa293ad6ed30193ad8d8e462ddc');
insert into tmp_loanproduct_penalty_charge_mapping values('MicrocreditoM','Mora diaria MicrocreditoM','8a4442a98212792801821331924c521d');
insert into tmp_loanproduct_penalty_charge_mapping values('Microcredito','Mora diaria Microcredito','8a44488480ff61ab018105dcd82344ca');
insert into tmp_loanproduct_penalty_charge_mapping values('Credito_Rotativo','Mora diaria crédito rotativo','8a4426a98a4d0efb018a4d7c210b5b66');
insert into tmp_loanproduct_penalty_charge_mapping values('Nano_cred_sem','Mora diaria Nano Credito Semanal','8a4440af8f1a7b2e018f1c6bd0c64b14');
insert into tmp_loanproduct_penalty_charge_mapping values('Microcredito_B','Mora diaria Microcredito B','8a4431d691336a4c019134e082030c38');
insert into tmp_loanproduct_penalty_charge_mapping values('Bajo_monto','Mora diaria Bajo monto','8a44504485cb20640185cbecc09457bc');
insert into tmp_loanproduct_penalty_charge_mapping values('Nano_cred_dia','Mora diaria Nano Credito Diario','8a4451df8f1c428e018f1cd4c19f02f0');


-- Create mambu to fineract charge names mapping
CREATE TABLE public.tmp_charge_mapping (
	ea_charge_name varchar null,
	fineract_charge_name varchar null,
	fineract_iva_charge_name varchar null
);
alter table tmp_charge_mapping add column charge_type varchar;

insert into tmp_charge_mapping values('Comisión MiPyme (>= 4SMMLV)','Comision Mi Pyme >= 4SMLV','IVA Comision Mi Pyme >= 4SMLV');
insert into tmp_charge_mapping values('Comision MiPyme (< 4SMMLV)','Comision Mi Pyme < 4SMLV','IVA Comision Mi Pyme < 4SMLV');
insert into tmp_charge_mapping values('Seguro de vida','Seguro de Vida','IVA Seguro de Vida');
insert into tmp_charge_mapping values('MiPymeMes6Menor','MiPymeMes6Menor','IVA_MiPymeMes6Menor');
insert into tmp_charge_mapping values('MiPymeMes9Menor','MiPymeMes9Menor','IVA_MiPymeMes9Menor');
insert into tmp_charge_mapping values('MiPymeMes3Menor','MiPymeMes3Menor','IVA_MiPymeMes3Menor');
insert into tmp_charge_mapping values('MiPymeMes9Mayor','MiPymeMes9Mayor','IVA_MiPymeMes9Mayor');
insert into tmp_charge_mapping values('MiPymeMes2Menor','MiPymeMes2Menor','IVA_MiPymeMes2Menor');
insert into tmp_charge_mapping values('MiPymeMes6Mayor','MiPymeMes6Mayor','IVA_MiPymeMes6Mayor');

update tmp_charge_mapping set charge_type = 'NORMAL';

select * from tmp_charge_mapping;

update m_product_loan set start_date = '2021-01-01' where start_date is not null;

-- create table to save migration response for multi disbursal loans
CREATE TABLE public.tmp_migration_response (
	id serial4 NOT NULL,
	status varchar(10) NULL,
	activity varchar(10) NULL,
	response text NULL,
	"timestamp" timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	loanid varchar NULL,
	productid varchar NULL,
	request varchar NULL,
	summary varchar NULL,
	CONSTRAINT migration_response_pkey PRIMARY KEY (id)
);

--truncate tmp_migration_response restart identity;


select * from m_charge where name = 'Flat Charge';
-- First Check if Flat Charge already exists or not. Do not run the insert query if it already exists

INSERT INTO public.m_charge
("name", currency_code, charge_applies_to_enum, charge_time_enum, charge_calculation_enum, charge_payment_mode_enum, amount, fee_on_day, fee_interval, fee_on_month, is_penalty, is_active, is_deleted, min_cap, max_cap, fee_frequency, is_free_withdrawal, free_withdrawal_charge_frequency, restart_frequency, restart_frequency_enum, is_payment_type, payment_type_id, income_or_liability_account_id, tax_group_id, grace_on_charge_period_enum, grace_on_charge_period_amount, parent_charge_id, interest_rate_id, insurance_name, insurance_charged_as, insurance_company, insurer_name, insurance_code, insurance_plan, base_value, vat_value, total_value, deadline, is_get_percentage_from_table, days_in_arrears, tmp_name)
VALUES('Flat Charge', 'COP', 1, 2, 1030, 0, 10.00000000, NULL, NULL, NULL, false, true, false, NULL, NULL, NULL, false, NULL, NULL, NULL, false, NULL, NULL, NULL, 1, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, NULL, 'Flat Charge');

-- Check that these charges below exist
select * from m_charge where name = 'MiPymeMes2Menor';

update m_charge set parent_charge_id = (select id from m_charge where name = 'MiPymeMes2Menor') where name = 'IVA_MiPymeMes2Menor';
update m_charge set parent_charge_id = (select id from m_charge where name = 'MiPymeMes3Menor') where name = 'IVA_MiPymeMes3Menor';
update m_charge set parent_charge_id = (select id from m_charge where name = 'MiPymeMes6Menor') where name = 'IVA_MiPymeMes6Menor';
update m_charge set parent_charge_id = (select id from m_charge where name = 'MiPymeMes9Menor') where name = 'IVA_MiPymeMes9Menor';
update m_charge set parent_charge_id = (select id from m_charge where name = 'MiPymeMes6Mayor') where name = 'IVA_MiPymeMes6Mayor';
update m_charge set parent_charge_id = (select id from m_charge where name = 'MiPymeMes9Mayor') where name = 'IVA_MiPymeMes9Mayor';

-- change charge names because loan import tool does not recognize special characters
select * from m_charge order by id;

alter table m_charge add column tmp_name varchar;


	update m_charge set tmp_name = name;
	update m_charge set name = replace(name, '<', '_');
	update m_charge set name = replace(name, '>', '__');
	update m_charge set name = replace(name, '=', '___');
	update m_charge set name = replace(name, '(', '_');
	update m_charge set name = replace(name, ')', '__');



update m_client mc
	set activation_date = '2021-01-01',
	office_joining_date = '2021-01-01',
	submittedon_date = '2021-01-01';

UPDATE public.m_charge
	SET is_deleted=false
	WHERE id=14;
UPDATE public.m_charge
	SET is_deleted=false
	WHERE id=15;


-- -- Check if Mifos and Bancos channel are present in c_channel table and are associated with all products in m_loan_product_channel table. Execute below if missing

select * from custom.c_channel;

-- if not then create
INSERT INTO custom.c_channel
(hash, "name", description, active, channel_type)
VALUES('Mifos', 'Mifos', 'Mifos', true, 1);
INSERT INTO custom.c_channel
(hash, "name", description, active, channel_type)
VALUES('1ae8d4db830eed577c6023998337d0hags546f1a3ba08e5df1ef0d1673431a3', 'Bancos', 'Bancos', true, 2);
INSERT INTO custom.c_channel
(hash, "name", description, active, channel_type)
VALUES('Mifos', 'Mifos', 'Mifos', true, 2);


INSERT INTO m_loan_product_channel
                (loan_product_id, channel_id)
                select mpl.ID as loan_product_id, ch.id as channel_id
                from public.m_product_loan mpl
                inner join custom.c_channel ch on ch.id in(select id from custom.c_channel where id not in (select channel_id from m_loan_product_channel));


--- check min and max interest rate for products. Adjust the rates in fineract based on the min max limit from mambu in below query and update fineract rate accordingly
select id, name, ml.nominal_interest_rate_per_period , ml.min_nominal_interest_rate_per_period , ml.max_nominal_interest_rate_per_period 
from m_product_loan ml
-- where id in (14, 12,13
order by name;

with cte as (
select tlp."ENCODEDKEY", tlp."ID" productname,
case
                    when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_DAY' then tcm."INTERESTRATE"*365
                    when tlp."ID" = 'Nano_cred_sem' then  tcm."INTERESTRATE"*52
		when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_MONTH' then tcm."INTERESTRATE"*12	
                   else tcm."INTERESTRATE"
                end as rate
                from tmp_loanaccount tcm
                join tmp_loan_product tlp on tlp."ENCODEDKEY" = tcm."PRODUCTTYPEKEY"
                where tcm."ACCOUNTSTATE" in ('ACTIVE', 'ACTIVE_IN_ARREARS')
)
select cte.productname, min(rate), max(rate)
from cte
group by cte.productname;

---   Check min and max principal amounts for loan product. Adjust the rates in fineract based on the min max limit from mambu in below query and update fineract rate accordingly
select id, name, ml.principal_amount , ml.min_principal_amount , ml.max_principal_amount 
from m_product_loan ml
--where id not in (14, 12,13)
order by name;

with cte as (
select tlp."ENCODEDKEY", tlp."ID" productname, tcm."LOANAMOUNT"
                from tmp_loanaccount tcm
                join tmp_loan_product tlp on tlp."ENCODEDKEY" = tcm."PRODUCTTYPEKEY"
                where tcm."ACCOUNTSTATE" in ('ACTIVE', 'ACTIVE_IN_ARREARS')
)
select cte."ENCODEDKEY", cte.productname, min("LOANAMOUNT"), max("LOANAMOUNT")
from cte
group by cte."ENCODEDKEY", cte.productname
order by productname;


-- copy loan submit and approved dates
create table tmp_loan_dates as
select tcm."ENCODEDKEY",
case 
		when tcm."APPROVEDDATE" < td."DISBURSEMENTDATE" then to_date(to_char(tcm."APPROVEDDATE", 'dd mm yyyy'), 'dd mm yyyy')
		else to_date(to_char(td."DISBURSEMENTDATE", 'dd mm yyyy'), 'dd mm yyyy')
	end as submit_date,
	case 
		when tcm."APPROVEDDATE" < td."DISBURSEMENTDATE" then to_date(to_char(tcm."APPROVEDDATE", 'dd mm yyyy'), 'dd mm yyyy')
		else to_date(to_char(td."DISBURSEMENTDATE", 'dd mm yyyy'), 'dd mm yyyy')
	end as approved_date
from tmp_loanaccount tcm
join tmp_disbursementdetails td on td."ENCODEDKEY" = tcm."DISBURSEMENTDETAILSKEY";


-- This loan charge has a space in name at the start

UPDATE public.m_charge
	SET tmp_name='Capital Pendiente Mi Pyme < 4SMLV',"name"='Capital Pendiente Mi Pyme _ 4SMLV'
	WHERE id=32;

select * from m_charge where id = 32;

-- DISABLE JOBS
update job set is_active = false;

--1019 + 930 + 352
select count(*) from m_loan where loan_status_id = 300;

update campos_cliente_persona set "Cupo solicitado" = 20000000, "Cupo aprobado" = 20000000;

select * from m_loan ml order by id;

select * from tmp_loanaccount tl where tl."ID" = '0038781591';

select * from tmp_loan_repayment_schedule tlrs where tlrs."PARENTACCOUNTKEY" = '8a44359f95aaf0f40195abb6af260535';

select tl."ID", tl."REPAYMENTINSTALLMENTS", COUNT(tlrs."STATE") paid_installments, (tl."REPAYMENTINSTALLMENTS" - COUNT(tlrs."STATE")) outstanding_installments from tmp_loanaccount tl join tmp_loan_repayment_schedule tlrs on tl."ENCODEDKEY" = tlrs."PARENTACCOUNTKEY"
where tlrs."STATE" = 'PAID' group by tl."ID", tl."REPAYMENTINSTALLMENTS";


select tl."ID", MIN(tlrs."DUEDATE")::date first_repayment_date from tmp_loanaccount tl join tmp_loan_repayment_schedule tlrs on tl."ENCODEDKEY" = tlrs."PARENTACCOUNTKEY"
where tlrs."STATE" != 'PAID' and tl."ID" = '0038781591' group by tl."ID";


-- EA-357 This loan has incorrect approval amount in mambu
UPDATE public.tmp_loanaccount
	SET "LOANAMOUNT"=6000000.0000000000
	WHERE "ID"='0492158972';
-- Import Loan Accounts
-- Query to load loan accounts through loan import tool.
-- Download loan template. Change external id column to text
-- import loans from Bajo Monto,  Lib Inv, Credito Com_PN and microB products

-- -==== NEW QUERIES TO CREATE LOANS FOR THE OUTSTANDING BALANCES =====-- 

-- We need to reduce the minimum and maximum principal amounts for all loan products (maximum because some loans have accrued penalties without any repayments made)
update m_product_loan set min_principal_amount = null, max_principal_amount = null; -- we'll use a back up to set these back or copy from Mambu
update m_product_loan set nominal_interest_rate_per_period = null, max_nominal_interest_rate_per_period = null;

select * from m_charge mc order by id desc;

-- Add a column to hold the current balance of the loan
alter table tmp_loanaccount add column TOTAL_OUTSTANDING decimal(19,6);

-- Update the total outstanding for each loan
update tmp_loanaccount tl set TOTAL_OUTSTANDING = tl."PRINCIPALBALANCE";
-- + tl."INTERESTBALANCE" + tl."FEESBALANCE" + tl."PENALTYBALANCE";


-- Add a column to hold the number of outstanding installments
alter table tmp_loanaccount add column NO_OF_OUTSTANDING_INSTALLMENTS int;

-- Update the number of oustanding installments
update tmp_loanaccount tcm
set NO_OF_OUTSTANDING_INSTALLMENTS = (
	select coalesce((tl."REPAYMENTINSTALLMENTS" - COUNT(tlrs."STATE")), tl."REPAYMENTINSTALLMENTS") outstanding_installments 
	from tmp_loanaccount tl 
	join tmp_loan_repayment_schedule tlrs on tl."ENCODEDKEY" = tlrs."PARENTACCOUNTKEY"
	where tlrs."STATE" = 'PAID' and tcm."ID" = tl."ID" group by tl."ID", tl."REPAYMENTINSTALLMENTS"
);
-- Set installment count to original installments if none is paid off
update tmp_loanaccount set NO_OF_OUTSTANDING_INSTALLMENTS = "REPAYMENTINSTALLMENTS" where NO_OF_OUTSTANDING_INSTALLMENTS is null;

select * from tmp_loanaccount tl;

select * from m_loan where loan_status_id = 300;

-- Add a column to hold the new disbursement date
alter table tmp_loanaccount add column NEW_DISBURSEMENT_DATE date;
-- Set the disbursement date: this should be the duedate of the last paid-off installment
update tmp_loanaccount tcm
set NEW_DISBURSEMENT_DATE = (
	select MAX(tlrs."DUEDATE")::date disbursement_date from tmp_loanaccount tl join tmp_loan_repayment_schedule tlrs on tl."ENCODEDKEY" = tlrs."PARENTACCOUNTKEY"
where tlrs."STATE" = 'PAID' and tcm."ID" = tl."ID" group by tl."ID"
);

-- Add a column to hold the new first repayment date
alter table tmp_loanaccount add column NEW_FIRST_REPAYMENT_DATE date;
-- set the first repayment date
update tmp_loanaccount tcm
set NEW_FIRST_REPAYMENT_DATE = (
	select MIN(tlrs."DUEDATE")::date first_repayment_date from tmp_loanaccount tl join tmp_loan_repayment_schedule tlrs on tl."ENCODEDKEY" = tlrs."PARENTACCOUNTKEY"
where tlrs."STATE" != 'PAID' and tcm."ID" = tl."ID" group by tl."ID"
);

select * from tmp_loanaccount tl where NEW_FIRST_REPAYMENT_DATE is null;
-- Default to disbursement date in disbursement details in the new one is null, in the script to populate the import file

-- -new query for outstanding loan balances
select distinct
	(select REPLACE(mo.name,' ', '_') from m_office mo where id = 1) as office_name,
	'Individual' as loan_type,
	concat(trim(mc.display_name), '(', mc.id , ')') as client_name,
	mc.external_id as client_external_id,
	REpLACE(mpl.name, ' ', '_') as product_name,
	'' loan_officer,
	-- td.disbursementdate as submit_date,
	-- td.disbursementdate as approved_date,
	-- td.disbursementdate as disbursement_date,
	to_date(to_char(coalesce(tcm.new_disbursement_date , td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as submit_date,
	to_date(to_char(coalesce(tcm."new_disbursement_date", td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as approved_date,
	to_date(to_char(coalesce(tcm."new_disbursement_date", td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as disbursement_date,
	'' payment_type,
	'' fund,
	tcm."LOANAMOUNT" as principal_amount,
	coalesce(tcm.no_of_outstanding_installments, 1) as no_of_repayments,
	tcm."REPAYMENTPERIODCOUNT"  repaid_every,
	tcm."REPAYMENTPERIODUNIT"  repay_freq,
	coalesce(tcm.no_of_outstanding_installments, 1) loan_term,
	tcm."REPAYMENTPERIODUNIT" loan_term_freq,
	-- tcm.INTERESTRATE   as nominal_interest_rate,
	case
		when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_DAY' then tcm."INTERESTRATE"*365
                    when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_WEEK' then  tcm."INTERESTRATE"*52
                    when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_MONTH' then  tcm."INTERESTRATE"*12
                   else tcm."INTERESTRATE"
	end	 nominal_interest_rate,	
	'Per Year' as interest_rate_per_month,
	'Equal installments' as ammortization,
	'Declining Balance' as interest_method,
	case
		when mpl.interest_calculated_in_period_enum = 0 then 'Daily'
		else 'Same as repayment period'
	end interest_calculation_period,
	'0' arrears_tolerance,
	mpl.loan_transaction_strategy_code repayment_strategy,
	0 grace_on_principal_pmt,
	0 grace_on_interest_pmt,
	0 interest_free_period,
	'' interest_charged_from, 
	coalesce(tcm.new_first_repayment_date, td."FIRSTREPAYMENTDATE") first_repayment_on,
	'' amount_repaid,
	'' date_last_repayment,
	'' repayment_type,
	tcm.total_outstanding as approved_principal,
	'',
	'',
	tcm."ID"  as external_id
	-- first_charge.charge_name first_charge, first_charge.iva_name first_iva,
	-- second_charge.charge_name second_charge, second_charge.iva_name second_iva,
	-- replace(first_charge.charge_name, ' ', '_'), first_charge.fee_percentage, '', replace(first_charge.iva_name, ' ', '_'), first_charge.iva_percentage, '',
	-- replace(second_charge.charge_name, ' ', '_'), second_charge.fee_percentage, replace(second_charge.iva_name, ' ', '_'), second_charge.iva_percentage,
	-- ''
	-- nit, code, cre_numerocredito::varchar, cli_nroid, 
-- 	cre_fechafinancia
from 
	tmp_loanaccount tcm
	join tmp_loanproduct_mapping tlm on tlm.ea_product_key = tcm."PRODUCTTYPEKEY"
	join m_product_loan mpl on mpl.name = tlm.mifos_product_name
	join tmp_disbursementdetails td on td."ENCODEDKEY" = tcm."DISBURSEMENTDETAILSKEY"
	join tmp_cliente_migrar tcm2 on tcm2.ENCODEDKEY = tcm."ACCOUNTHOLDERKEY"
	left join m_client mc on mc.external_id = tcm2.ID 
	left join lateral (
		select tlc.loankey, mc1.id charge_id, mc1.name charge_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.id
		end as iva_id,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.name
		end as iva_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else 19
		end as iva_percentage,
		case
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE' then tlc.fee_percentage
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE_NUMBER_OF_INSTALLMENTS' then tlc.fee_percentage/coalesce(tcm.no_of_outstanding_installments, 1)		
		end as fee_percentage
		from tmp_loan_charges tlc
		left join tmp_charge_mapping charge_mapping1 on lower(charge_mapping1.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join tmp_charge_mapping charge_mapping2 on lower(charge_mapping2.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join m_charge mc1 on lower(mc1.tmp_name) = lower(charge_mapping1.fineract_charge_name)
        left join m_charge mc2 on lower(mc2.tmp_name) = lower(charge_mapping2.fineract_iva_charge_name) 
         where tlc.loankey = tcm."ENCODEDKEY"
        order by tlc."NAME" desc limit 1
	) first_charge on first_charge.loankey = tcm."ENCODEDKEY"
	left join lateral (
		select tlc.loankey, mc1.id charge_id, mc1.name charge_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.id
		end as iva_id,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.name
		end as iva_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else 19
		end as iva_percentage,
		case
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE' then tlc.fee_percentage
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE_NUMBER_OF_INSTALLMENTS' then tlc.fee_percentage/coalesce(tcm.no_of_outstanding_installments, 1)			
		end as fee_percentage		
		from tmp_loan_charges tlc
		left join tmp_charge_mapping charge_mapping1 on lower(charge_mapping1.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join tmp_charge_mapping charge_mapping2 on lower(charge_mapping2.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join m_charge mc1 on lower(mc1.tmp_name) = lower(charge_mapping1.fineract_charge_name)
        left join m_charge mc2 on lower(mc2.tmp_name) = lower(charge_mapping2.fineract_iva_charge_name) 
        where tlc.loankey = tcm."ENCODEDKEY"
        order by tlc."NAME" asc limit 1
	) second_charge on second_charge.loankey = tcm."ENCODEDKEY"
	  where  mpl.id < 9
	  -- and tcm.total_outstanding > 0
	  -- and tcm."ID" not in (select external_id from m_loan)
	 -- and tcm."ID" in ('9888159019')
	 order by tcm."ID";
	
	
	-- truncate table m_loan restart identity cascade;
	-- truncate table m_document restart identity cascade;
	
	
	select * from m_loan_charge mlc where mlc.loan_id = 2;

select * from m_loan_transaction order by id desc;

select * from m_loan where id = 446;
	
	
	select * from tmp_cliente_migrar;

select * from m_loan ml;
	
	select * from tmp_loanaccount tl where tl."ID" = '0039682591';

select * from tmp_loan_repayment_schedule tlrs where "PARENTACCOUNTKEY" = '8a44359f95aaf0f40195abb6af260535' order by "DUEDATE";
	
-- Now year <2024
update c_configuration set value = 1000000 where name = 'SMLV';
	
select distinct
	(select REPLACE(mo.name,' ', '_') from m_office mo where id = 1) as office_name,
	'Individual' as loan_type,
	concat(trim(mc.display_name), '(', mc.id , ')') as client_name,
	mc.external_id as client_external_id,
	REpLACE(mpl.name, ' ', '_') as product_name,
	'' loan_officer,
	-- td.disbursementdate as submit_date,
	-- td.disbursementdate as approved_date,
	-- td.disbursementdate as disbursement_date,
	to_date(to_char(coalesce(tcm.new_disbursement_date , td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as submit_date,
	to_date(to_char(coalesce(tcm."new_disbursement_date", td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as approved_date,
	to_date(to_char(coalesce(tcm."new_disbursement_date", td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as disbursement_date,
	'' payment_type,
	'' fund,
	tcm."LOANAMOUNT" as principal_amount,
	coalesce(tcm.no_of_outstanding_installments, 1) as no_of_repayments,
	tcm."REPAYMENTPERIODCOUNT"  repaid_every,
	tcm."REPAYMENTPERIODUNIT"  repay_freq,
	coalesce(tcm.no_of_outstanding_installments, 1) loan_term,
	tcm."REPAYMENTPERIODUNIT" loan_term_freq,
	-- tcm.INTERESTRATE   as nominal_interest_rate,
	case
		when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_DAY' then tcm."INTERESTRATE"*365
                    when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_WEEK' then  tcm."INTERESTRATE"*52
                    when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_MONTH' then  tcm."INTERESTRATE"*12
                   else tcm."INTERESTRATE"
	end	 nominal_interest_rate,	
	'Per Year' as interest_rate_per_month,
	'Equal installments' as ammortization,
	'Declining Balance' as interest_method,
case
		when mpl.interest_calculated_in_period_enum = 0 then 'Daily'
		else 'Same as repayment period'
	end interest_calculation_period,
	'0' arrears_tolerance,
	mpl.loan_transaction_strategy_code repayment_strategy,
	0 grace_on_principal_pmt,
	0 grace_on_interest_pmt,
	0 interest_free_period,
	'' interest_charged_from, 
	coalesce(tcm.new_first_repayment_date, td."FIRSTREPAYMENTDATE") first_repayment_on,
	'' amount_repaid,
	'' date_last_repayment,
	'' repayment_type,
	tcm.total_outstanding as approved_principal,
	'',
	'',
	tcm."ID"  as external_id
	-- first_charge.charge_name first_charge, first_charge.iva_name first_iva,
	-- second_charge.charge_name second_charge, second_charge.iva_name second_iva,
	-- replace(first_charge.charge_name, ' ', '_'), first_charge.fee_percentage, '', replace(first_charge.iva_name, ' ', '_'), first_charge.iva_percentage, '',
	-- case 
	--	when mpl.id = 8 then replace(mipyme_charge.charge_name, ' ', '_')
	--	else replace(second_charge.charge_name, ' ', '_')
	-- end,
	-- case 
	--	when mpl.id = 8 then mipyme_charge.fee_percentage
	--	else second_charge.fee_percentage
	-- end, '',
	-- case 
	--	when mpl.id = 8 then replace(mipyme_charge.iva_name, ' ', '_')
	--	else replace(second_charge.iva_name, ' ', '_')
	-- end,
	-- case 
	--	when mpl.id = 8 then mipyme_charge.iva_percentage
	--	else second_charge.iva_percentage
	-- end,
	-- ''
	-- nit, code, cre_numerocredito::varchar, cli_nroid, 
-- 	cre_fechafinancia
from 
	tmp_loanaccount tcm
	join tmp_loanproduct_mapping tlm on tlm.ea_product_key = tcm."PRODUCTTYPEKEY"
	join m_product_loan mpl on mpl.name = tlm.mifos_product_name
	join tmp_disbursementdetails td on td."ENCODEDKEY" = tcm."DISBURSEMENTDETAILSKEY"
	join tmp_cliente_migrar tcm2 on tcm2.ENCODEDKEY = tcm."ACCOUNTHOLDERKEY"
	join m_client mc on mc.external_id = tcm2.ID 
	left join lateral (
		select tlc.loankey, mc1.id charge_id, mc1.name charge_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.id
		end as iva_id,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.name
		end as iva_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else 19
		end as iva_percentage,
		case
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE' then tlc.fee_percentage
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE_NUMBER_OF_INSTALLMENTS' then tlc.fee_percentage/coalesce(tcm.no_of_outstanding_installments, 1)
		end as fee_percentage
		from tmp_loan_charges tlc
		left join tmp_charge_mapping charge_mapping1 on lower(charge_mapping1.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join tmp_charge_mapping charge_mapping2 on lower(charge_mapping2.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join m_charge mc1 on lower(mc1.tmp_name) = lower(charge_mapping1.fineract_charge_name)
        left join m_charge mc2 on lower(mc2.tmp_name) = lower(charge_mapping2.fineract_iva_charge_name) 
         where tlc.loankey = tcm."ENCODEDKEY"
        order by tlc."NAME" desc limit 1
	) first_charge on first_charge.loankey = tcm."ENCODEDKEY"
	left join lateral (
		select tlc.loankey, mc1.id charge_id, mc1.name charge_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.id
		end as iva_id,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.name
		end as iva_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else 19
		end as iva_percentage,
		case
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE' then tlc.fee_percentage
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE_NUMBER_OF_INSTALLMENTS' then tlc.fee_percentage/coalesce(tcm.no_of_outstanding_installments, 1)
		end as fee_percentage		
		from tmp_loan_charges tlc
		left join tmp_charge_mapping charge_mapping1 on lower(charge_mapping1.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join tmp_charge_mapping charge_mapping2 on lower(charge_mapping2.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join m_charge mc1 on lower(mc1.tmp_name) = lower(charge_mapping1.fineract_charge_name)
        left join m_charge mc2 on lower(mc2.tmp_name) = lower(charge_mapping2.fineract_iva_charge_name) 
        where tlc.loankey = tcm."ENCODEDKEY"
        order by tlc."NAME" asc limit 1
	) second_charge on second_charge.loankey = tcm."ENCODEDKEY"
	left join lateral (
		select tcm."ENCODEDKEY" loankey, mc1.id charge_id, mc1.name charge_name,
		mc2.id iva_id,
		 mc2.name iva_name,
		mc2.amount iva_percentage,
		mc1.amount fee_percentage		
		from m_charge mc1
		join m_charge mc2 on mc2.parent_charge_id = mc1.id
		where 
		mpl.id = 8
		and case
			when tcm."LOANAMOUNT" < 5694000 then mc1.tmp_name = 'Capital Pendiente Mi Pyme < 4SMLV'
			when tcm."LOANAMOUNT" >= 5694000 then mc1.tmp_name = 'Capital Pendiente Mi Pyme >= 4SMLV'		
			else mc1.tmp_name = 'none'
		end
		and case
			when tcm."LOANAMOUNT" < 5694000 then mc2.tmp_name = 'IVA Capital Pendiente Mi Pyme < 4SMLV'
			when tcm."LOANAMOUNT" >= 5694000 then  mc2.tmp_name = 'IVA Capital Pendiente Mi Pyme >= 4SMLV'			
			else mc2.tmp_name = 'none'
		end
		) mipyme_charge on mipyme_charge.loankey = tcm."ENCODEDKEY"
	where  mpl.id in (9,10) and trim(to_char(td."DISBURSEMENTDATE",'YYYY')) in ('2021','2022','2023')
	  -- and tcm.id not in ('5926921620','5798558581','9043209288','4911099153','6171223614','2161289444','5775498843','8187153983','0022032372','7770432985','1426513226','3351179832','3532209966','8111747402','3780860737','5531840773','7534435854','0372390302','0673688145','4741881842','0890400726','6782519014','4761121438','8915931224','5966130912','8333585890','6241295657','5823029570','1515755714','5324318143','1090373796','6210443011','8162545940')
	and tcm."ID" not in (select external_id from m_loan)
	  -- and tcm.total_outstanding > 0
		order by tcm."ID";
	
	
	
	-- Now year 2024
update c_configuration set value = 1300000 where name = 'SMLV';

select distinct
	(select REPLACE(mo.name,' ', '_') from m_office mo where id = 1) as office_name,
	'Individual' as loan_type,
	concat(trim(mc.display_name), '(', mc.id , ')') as client_name,
	mc.external_id as client_external_id,
	REpLACE(mpl.name, ' ', '_') as product_name,
	'' loan_officer,
	-- td.disbursementdate as submit_date,
	-- td.disbursementdate as approved_date,
	-- td.disbursementdate as disbursement_date,
	to_date(to_char(coalesce(tcm.new_disbursement_date , td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as submit_date,
	to_date(to_char(coalesce(tcm."new_disbursement_date", td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as approved_date,
	to_date(to_char(coalesce(tcm."new_disbursement_date", td."DISBURSEMENTDATE"), 'dd mm yyyy'), 'dd mm yyyy') as disbursement_date,
	'' payment_type,
	'' fund,
	tcm."LOANAMOUNT" as principal_amount,
	coalesce(tcm.no_of_outstanding_installments, 1) as no_of_repayments,
	tcm."REPAYMENTPERIODCOUNT"  repaid_every,
	tcm."REPAYMENTPERIODUNIT"  repay_freq,
	coalesce(tcm.no_of_outstanding_installments, 1) loan_term,
	tcm."REPAYMENTPERIODUNIT" loan_term_freq,
	-- tcm.INTERESTRATE   as nominal_interest_rate,
	case
		when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_DAY' then tcm."INTERESTRATE"*365
                    when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_WEEK' then  tcm."INTERESTRATE"*52
                    when tcm."INTERESTCHARGEFREQUENCY" = 'EVERY_MONTH' then  tcm."INTERESTRATE"*12
                   else tcm."INTERESTRATE"
	end	 nominal_interest_rate,	
	'Per Year' as interest_rate_per_month,
	'Equal installments' as ammortization,
	'Declining Balance' as interest_method,
case
		when mpl.interest_calculated_in_period_enum = 0 then 'Daily'
		else 'Same as repayment period'
	end interest_calculation_period,
	'0' arrears_tolerance,
	mpl.loan_transaction_strategy_code repayment_strategy,
	0 grace_on_principal_pmt,
	0 grace_on_interest_pmt,
	0 interest_free_period,
	'' interest_charged_from, 
	coalesce(tcm.new_first_repayment_date, td."FIRSTREPAYMENTDATE") first_repayment_on,
	'' amount_repaid,
	'' date_last_repayment,
	'' repayment_type,
	tcm.total_outstanding as approved_principal,
	'',
	'',
	tcm."ID"  as external_id
	-- first_charge.charge_name first_charge, first_charge.iva_name first_iva,
	-- second_charge.charge_name second_charge, second_charge.iva_name second_iva,
	-- replace(first_charge.charge_name, ' ', '_'), first_charge.fee_percentage, '', replace(first_charge.iva_name, ' ', '_'), first_charge.iva_percentage, '',
	-- case 
	--	when mpl.id = 8 then replace(mipyme_charge.charge_name, ' ', '_')
	--	else replace(second_charge.charge_name, ' ', '_')
	-- end,
	-- case 
	--	when mpl.id = 8 then mipyme_charge.fee_percentage
	--	else second_charge.fee_percentage
	-- end, '',
	-- case 
	--	when mpl.id = 8 then replace(mipyme_charge.iva_name, ' ', '_')
	--	else replace(second_charge.iva_name, ' ', '_')
	-- end,
	-- case 
	--	when mpl.id = 8 then mipyme_charge.iva_percentage
	--	else second_charge.iva_percentage
	-- end,
	-- ''
	-- nit, code, cre_numerocredito::varchar, cli_nroid, 
-- 	cre_fechafinancia
from 
	tmp_loanaccount tcm
	join tmp_loanproduct_mapping tlm on tlm.ea_product_key = tcm."PRODUCTTYPEKEY"
	join m_product_loan mpl on mpl.name = tlm.mifos_product_name
	join tmp_disbursementdetails td on td."ENCODEDKEY" = tcm."DISBURSEMENTDETAILSKEY"
	join tmp_cliente_migrar tcm2 on tcm2.ENCODEDKEY = tcm."ACCOUNTHOLDERKEY"
	join m_client mc on mc.external_id = tcm2.ID 
	left join lateral (
		select tlc.loankey, mc1.id charge_id, mc1.name charge_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.id
		end as iva_id,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.name
		end as iva_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else 19
		end as iva_percentage,
		case
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE' then tlc.fee_percentage
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE_NUMBER_OF_INSTALLMENTS' then tlc.fee_percentage/coalesce(tcm.no_of_outstanding_installments, 1)
		end as fee_percentage
		from tmp_loan_charges tlc
		left join tmp_charge_mapping charge_mapping1 on lower(charge_mapping1.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join tmp_charge_mapping charge_mapping2 on lower(charge_mapping2.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join m_charge mc1 on lower(mc1.tmp_name) = lower(charge_mapping1.fineract_charge_name)
        left join m_charge mc2 on lower(mc2.tmp_name) = lower(charge_mapping2.fineract_iva_charge_name) 
         where tlc.loankey = tcm."ENCODEDKEY"
        order by tlc."NAME" desc limit 1
	) first_charge on first_charge.loankey = tcm."ENCODEDKEY"
	left join lateral (
		select tlc.loankey, mc1.id charge_id, mc1.name charge_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.id
		end as iva_id,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else mc2.name
		end as iva_name,
		case
			when lower(mc1.name) = 'seguro de vida' then null
			else 19
		end as iva_percentage,
		case
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE' then tlc.fee_percentage
			when tlc."AMOUNTCALCULATIONMETHOD" = 'LOAN_AMOUNT_PERCENTAGE_NUMBER_OF_INSTALLMENTS' then tlc.fee_percentage/coalesce(tcm.no_of_outstanding_installments, 1)
		end as fee_percentage		
		from tmp_loan_charges tlc
		left join tmp_charge_mapping charge_mapping1 on lower(charge_mapping1.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join tmp_charge_mapping charge_mapping2 on lower(charge_mapping2.ea_charge_name) = lower(tlc."NAME") and charge_mapping1.charge_type = 'NORMAL'
		left join m_charge mc1 on lower(mc1.tmp_name) = lower(charge_mapping1.fineract_charge_name)
        left join m_charge mc2 on lower(mc2.tmp_name) = lower(charge_mapping2.fineract_iva_charge_name) 
        where tlc.loankey = tcm."ENCODEDKEY"
        order by tlc."NAME" asc limit 1
	) second_charge on second_charge.loankey = tcm."ENCODEDKEY"
	left join lateral (
		select tcm."ENCODEDKEY" loankey, mc1.id charge_id, mc1.name charge_name,
		mc2.id iva_id,
		 mc2.name iva_name,
		mc2.amount iva_percentage,
		mc1.amount fee_percentage		
		from m_charge mc1
		join m_charge mc2 on mc2.parent_charge_id = mc1.id
		where 
		mpl.id = 8
		and case
			when tcm."LOANAMOUNT" < 5694000 then mc1.tmp_name = 'Capital Pendiente Mi Pyme < 4SMLV'
			when tcm."LOANAMOUNT" >= 5694000 then mc1.tmp_name = 'Capital Pendiente Mi Pyme >= 4SMLV'		
			else mc1.tmp_name = 'none'
		end
		and case
			when tcm."LOANAMOUNT" < 5694000 then mc2.tmp_name = 'IVA Capital Pendiente Mi Pyme < 4SMLV'
			when tcm."LOANAMOUNT" >= 5694000 then  mc2.tmp_name = 'IVA Capital Pendiente Mi Pyme >= 4SMLV'			
			else mc2.tmp_name = 'none'
		end
		) mipyme_charge on mipyme_charge.loankey = tcm."ENCODEDKEY"
	where  mpl.id in (9,10) and trim(to_char(td."DISBURSEMENTDATE",'YYYY')) in ('2024')
	  -- and tcm.id not in ('5926921620','5798558581','9043209288','4911099153','6171223614','2161289444','5775498843','8187153983','0022032372','7770432985','1426513226','3351179832','3532209966','8111747402','3780860737','5531840773','7534435854','0372390302','0673688145','4741881842','0890400726','6782519014','4761121438','8915931224','5966130912','8333585890','6241295657','5823029570','1515755714','5324318143','1090373796','6210443011','8162545940')
	and tcm."ID" not in (select external_id from m_loan)
	  -- and tcm.total_outstanding > 0
		order by tcm."ID";
	
	
-- -==== END OF NEW QUERIES TO CREATE LOANS FOR THE OUTSTANDING BALANCES =====-- 

-- Custom Fields -- DATA TABLES

truncate table "Estado Cartera";

INSERT INTO public."Estado Cartera"
(loan_id, "Tipo Bloqueo_cd_Tipo Bloqueo", fecha_bloqueo, reestructurado, fecha_reestructurado, negociaciones, salvamento, 
obs_salvamento, "Casa Cobranzas Asignada_cd_Casa Cobranzas Asignada", created_at, updated_at, periodo_gracia, cuota_periodo_gracia)
select 
ml.id, mc_tipo_bloq.id, to_date(tc_block_fetcha.field_value, 'YYYY-MM-DD'), 
case 
	when tc_restructure.field_value = 'Si' then true
	else false
end restructure,
to_date(tc_restructure_fetcha.field_value, 'YYYY-MM-DD'), tc_negociaciones.field_id, 
case 
	when tc_salvamento.field_value = 'Si' then true
	else false
end salvamento,
tc_obs_salvamento.field_value, mc_casa_cobro.id, now(), now(), null, null
from
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_bloc on tc_bloc."ENCODEDKEY" = tl."ENCODEDKEY" and tc_bloc.field_id = 'Tipo_Bloqueo'
left join m_code_value mc_tipo_bloq on mc_tipo_bloq.code_value = tc_bloc.field_value and mc_tipo_bloq.code_id = (select id from m_code where code_name = 'Tipo Bloqueo' )
left join tmp_customfieldvalue tc_block_fetcha on tc_block_fetcha."ENCODEDKEY" = tl."ENCODEDKEY" and tc_block_fetcha.field_id = 'Fecha_Bloqueo'
left join tmp_customfieldvalue tc_restructure on tc_restructure."ENCODEDKEY" = tl."ENCODEDKEY" and tc_restructure.field_id = 'Reestructurado'
left join tmp_customfieldvalue tc_restructure_fetcha on tc_restructure_fetcha."ENCODEDKEY" = tl."ENCODEDKEY" and tc_restructure_fetcha.field_id = 'fecha_reestructurado'
left join tmp_customfieldvalue tc_negociaciones on tc_negociaciones."ENCODEDKEY" = tl."ENCODEDKEY" and tc_negociaciones.field_id = 'Negociaciones'
left join tmp_customfieldvalue tc_salvamento on tc_salvamento."ENCODEDKEY" = tl."ENCODEDKEY" and tc_salvamento.field_id = 'Salvamento'
left join tmp_customfieldvalue tc_obs_salvamento on tc_obs_salvamento."ENCODEDKEY" = tl."ENCODEDKEY" and tc_obs_salvamento.field_id = 'Obs_Salvamento'
left join tmp_customfieldvalue tc_casa_cobro on tc_casa_cobro."ENCODEDKEY" = tl."ENCODEDKEY" and tc_casa_cobro.field_id = 'casa_cobro'
left join m_code_value mc_casa_cobro on mc_casa_cobro.code_value = tc_casa_cobro.field_value and mc_casa_cobro.code_id = (select id from m_code where code_name = 'Casa Cobranzas Asignada')
-- where tl.encodedkey = '8a4457908b19b7f5018b1a1eeff96aa4'
order by ml.id;

truncate table "Validacion Operacion";

INSERT INTO public."Validacion Operacion"
(loan_id, correo_validado, no_celular_validado, alerta_direccion, credito_vigente, observacion_operaciones, "SiNoNoAplica_cd_retanqueo", fecha_retanqueo, "SiNoNoAplica_cd_Cliente_existe", 
"Tipo Credito_cd_Tipo Credito", created_at, updated_at)
select
ml.id, 
case 
	when tc_validado.field_value = 'Si' then true
	else false
end validato,
case 
	when tc_cel_validado.field_value = 'Si' then true
	else false
end cel_validato,
case 
	when tc_direccion.field_value = 'Si' then true
	else false
end direccion,
case 
	when tc_vignete.field_value = 'Si' then true
	else false
end vignete,
tc_operations.field_value, mc_retenquo.id, to_date(tc_fetcha_retenquo.field_value, 'YYYY-MM-DD'),
mc_clientexists.id, mc_credito.id, now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_validado on tc_validado."ENCODEDKEY" = tl."ENCODEDKEY" and tc_validado.field_id = 'Correo_Validado'
left join tmp_customfieldvalue tc_cel_validado on tc_cel_validado."ENCODEDKEY" = tl."ENCODEDKEY" and tc_cel_validado.field_id = 'No_Celular_Validado'
left join tmp_customfieldvalue tc_direccion on tc_direccion."ENCODEDKEY" = tl."ENCODEDKEY" and tc_direccion.field_id = 'Alerta_Dirección'
left join tmp_customfieldvalue tc_vignete on tc_vignete."ENCODEDKEY" = tl."ENCODEDKEY" and tc_vignete.field_id = 'Credito_Vigente'
left join tmp_customfieldvalue tc_operations on tc_operations."ENCODEDKEY" = tl."ENCODEDKEY" and tc_operations.field_id = 'Observación_Operaciones'
left join tmp_customfieldvalue tc_retenquo on tc_retenquo."ENCODEDKEY" = tl."ENCODEDKEY" and tc_retenquo.field_id = 'Retanqueo'
left join m_code_value mc_retenquo on mc_retenquo.code_value = tc_retenquo.field_value and mc_retenquo.code_id = (select id from m_code where code_name = 'SiNoNoAplica' )
left join tmp_customfieldvalue tc_fetcha_retenquo on tc_fetcha_retenquo."ENCODEDKEY" = tl."ENCODEDKEY" and tc_fetcha_retenquo.field_id = 'Fecha_Retanqueo'
left join tmp_customfieldvalue tc_clientexists on tc_clientexists."ENCODEDKEY" = tl."ENCODEDKEY" and tc_clientexists.field_id = 'Cliente_existe'
left join m_code_value mc_clientexists on mc_clientexists.code_value = tc_clientexists.field_value and mc_clientexists.code_id = (select id from m_code where code_name = 'SiNoNoAplica' )
left join tmp_customfieldvalue tc_credito on tc_credito."ENCODEDKEY" = tl."ENCODEDKEY" and tc_credito.field_id = 'Tipo_credito'
left join m_code_value mc_credito on mc_credito.code_value = tc_credito.field_value and mc_credito.code_id = (select id from m_code where code_name = tc_credito.field_name )
order by ml.id;

truncate table "Visita";

INSERT INTO public."Visita"
(loan_id, req_visita, "Resultado Visita_cd_Resultado Visita", "Canal de Visita_cd_Canal de Visita", "Causal Rechazo Visita_cd_Causal Rechazo Visita", entrevista_fallida, created_at, updated_at)
select 
ml.id, 
case 
	when tc_req.field_value = 'Si' then true
	else false
end req
, mc_result.id, mc_canal.id, mc_causal.id, tc_fillida.field_value, now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_req on tc_req."ENCODEDKEY" = tl."ENCODEDKEY" and tc_req.field_id = 'Req_Visita'
left join tmp_customfieldvalue tc_result on tc_result."ENCODEDKEY" = tl."ENCODEDKEY" and tc_result.field_id = 'Resultado_Visita'
left join m_code_value mc_result on mc_result.code_value = tc_result.field_value and mc_result.code_id = (select id from m_code where code_name = 'Resultado Visita' )
left join tmp_customfieldvalue tc_canal on tc_canal."ENCODEDKEY" = tl."ENCODEDKEY" and tc_canal.field_id = 'Canal_de_Visita'
left join m_code_value mc_canal on mc_canal.code_value = tc_canal.field_value and mc_canal.code_id = (select id from m_code where code_name = 'Canal de Visita' )
left join tmp_customfieldvalue tc_causal on tc_causal."ENCODEDKEY" = tl."ENCODEDKEY" and tc_causal.field_id = 'Causal_Rechazo'
left join m_code_value mc_causal on mc_causal.code_value = tc_causal.field_value and mc_causal.code_id = (select id from m_code where code_name = 'Causal Rechazo Visita' )
left join tmp_customfieldvalue tc_fillida on tc_fillida."ENCODEDKEY" = tl."ENCODEDKEY" and tc_fillida.field_id = 'Entrevista_Fallida'
order by ml.id;

truncate table "Validacion Contacta";

INSERT INTO public."Validacion Contacta"
(loan_id, "Usuario Asignado_cd_Usuario Asignado", "Correo Usuario Asignado_cd_Correo Usuario Asignado", fecha_inicio_contactabilidad, "Validacion Contactabilidad_cd_Validacion Contactabilidad", 
"Causal Rechazo Contactabilidad_cd_Causal Rechazo Contactabilida", contactabilidad_fallida, observacion_contactabilidad, contactabilidad_observ2, contactabilidad_observ3, 
telefono_de_contacto, fecha_fin_contactabilidad, created_at, updated_at)
select distinct
 ml.id,mc_user.id, mc_mail.id, to_date(tc_inicio.field_value, 'YYYY-MM-DD'), mc_contactabilidad.id, mc_contacta.id, tc_fillidia.field_value,tc_obs.field_value, tc_obs2.field_value, tc_obs3.field_value, 
tc_contacto.field_value, to_date(tc_fetcha.field_value, 'YYYY-MM-DD'), now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_user on tc_user."ENCODEDKEY" = tl."ENCODEDKEY" and tc_user.field_id = 'Usuario_Asignado'
left join m_code_value mc_user on mc_user.code_value = tc_user.field_value and mc_user.code_id = (select id from m_code where code_name = 'Usuario Asignado' )
left join tmp_customfieldvalue tc_mail on tc_mail."ENCODEDKEY" = tl."ENCODEDKEY" and tc_mail.field_id = 'mail_user'
left join m_code_value mc_mail on mc_mail.code_value = tc_mail.field_value and mc_mail.code_id = (select id from m_code where code_name = 'Correo Usuario Asignado' )
left join tmp_customfieldvalue tc_inicio on tc_inicio."ENCODEDKEY" = tl."ENCODEDKEY" and tc_inicio.field_id = 'Fecha_Inicio_Contactabilidad'
left join tmp_customfieldvalue tc_contactabilidad on tc_contactabilidad."ENCODEDKEY" = tl."ENCODEDKEY" and tc_contactabilidad.field_id = 'Contactabilidad'
left join m_code_value mc_contactabilidad on mc_contactabilidad.code_value = tc_contactabilidad.field_value and mc_contactabilidad.code_id = (select id from m_code where code_name = 'Validacion Contactabilidad' )
left join tmp_customfieldvalue tc_contacta on tc_contacta."ENCODEDKEY" = tl."ENCODEDKEY" and tc_contacta.field_id = 'Causal_Rechazo_Contacta'
left join m_code_value mc_contacta on mc_contacta.code_value = tc_contacta.field_value and mc_contacta.code_id = (select id from m_code where code_name = 'Causal Rechazo Contactabilidad' )
left join tmp_customfieldvalue tc_fillidia on tc_fillidia."ENCODEDKEY" = tl."ENCODEDKEY" and tc_fillidia.field_id = 'Contactabilidad_Fallida'
left join tmp_customfieldvalue tc_obs on tc_obs."ENCODEDKEY" = tl."ENCODEDKEY" and tc_obs.field_id = 'Observacion_Contactabilidad'
left join tmp_customfieldvalue tc_obs2 on tc_obs2."ENCODEDKEY" = tl."ENCODEDKEY" and tc_obs2.field_id = 'Contactabilidad_Observ2'
left join tmp_customfieldvalue tc_obs3 on tc_obs3."ENCODEDKEY" = tl."ENCODEDKEY" and tc_obs3.field_id = 'Contactabilidad_Observ3'
left join tmp_customfieldvalue tc_contacto on tc_contacto."ENCODEDKEY" = tl."ENCODEDKEY" and tc_contacto.field_id = 'Teléfono_de_Contacto'
left join tmp_customfieldvalue tc_fetcha on tc_fetcha."ENCODEDKEY" = tl."ENCODEDKEY" and tc_fetcha.field_id = 'Fecha_Fin_Contactabilidad'
where  -- mc_mail.id = 2510 or 
mc_mail.id is null
order by ml.id;

truncate table "Validacion Seguridad";

INSERT INTO public."Validacion Seguridad"
(loan_id, "Validacion Seguridad_cd_Validacion Seguridad", "Causal Rechazo Seguridad_cd_Causal Rechazo Seguridad", seguridad_fallida, observacion_seguridad, created_at, updated_at)
select
 ml.id, mc_segu.id, mc_canal.id, tc_fillida.field_value, tc_obs.field_value, now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_segu on tc_segu."ENCODEDKEY" = tl."ENCODEDKEY" and tc_segu.field_id = 'Validacion_Seguridad_'
left join m_code_value mc_segu on mc_segu.code_value = tc_segu.field_value and mc_segu.code_id = (select id from m_code where code_name = 'Validacion Seguridad' )
left join tmp_customfieldvalue tc_canal on tc_canal."ENCODEDKEY" = tl."ENCODEDKEY" and tc_canal.field_id = 'Causal_Rechazo_Seguridad'
left join m_code_value mc_canal on mc_canal.code_value = tc_canal.field_value and mc_canal.code_id = (select id from m_code where code_name = 'Causal Rechazo Seguridad' )
left join tmp_customfieldvalue tc_fillida on tc_fillida."ENCODEDKEY" = tl."ENCODEDKEY" and tc_fillida.field_id = 'Seguridad_Fallida'
left join tmp_customfieldvalue tc_obs on tc_obs."ENCODEDKEY" = tl."ENCODEDKEY" and tc_obs.field_id = 'Observacion_Seguridad'
order by ml.id;

truncate table "Detalle garantia";

INSERT INTO public."Detalle garantia"
(loan_id, aplica_garantia, "Tipo Garantía_cd_Tipo Garantía", "Tipo Producto_cd_Tipo Producto", porc_comision, porc_cobertura, numero_garantia, 
fecha_registro_garantia, fecha_reserva, 
fecha_cancelacion, "Estado Garantía_cd_Estado Garantía", obs_estado_garantia, numero_pagare, created_at, updated_at)
select
ml.id, 
case 
	when tc_aplica.field_value = 'Si' then true
	else false
end aplica,
 mc_tipo.id, mc_producto.id, tc_commission.field_value, tc_cobertura.field_value, tc_gaurantee.field_value, 
TO_TIMESTAMP(tc_fetcha.field_value, 'YYYY-MM-DDTHH24:MI:SS')::timestamp at time zone 'UTC' as d1
 , TO_TIMESTAMP(tc_reserve.field_value, 'YYYY-MM-DDTHH24:MI:SS')::timestamp at time zone 'UTC' as d2,
to_date(tc_cancel.field_value, 'YYYY-MM-DD'), mc_estado.id, tc_obs.field_value, tc_numero.field_value, now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_aplica on tc_aplica."ENCODEDKEY" = tl."ENCODEDKEY" and tc_aplica.field_id = 'aplica_garantia'
left join tmp_customfieldvalue tc_tipo on tc_tipo."ENCODEDKEY" = tl."ENCODEDKEY" and tc_tipo.field_id = 'tipo_garantia'
left join m_code_value mc_tipo on mc_tipo.code_value = tc_tipo.field_value and mc_tipo.code_id = (select id from m_code where code_name = 'Tipo Garantía' )
left join tmp_customfieldvalue tc_producto on tc_producto."ENCODEDKEY" = tl."ENCODEDKEY" and tc_producto.field_id = 'tipo_producto'
left join m_code_value mc_producto on mc_producto.code_value = tc_producto.field_value and mc_producto.code_id = (select id from m_code where code_name = 'Tipo Producto' )
left join tmp_customfieldvalue tc_commission on tc_commission."ENCODEDKEY" = tl."ENCODEDKEY" and tc_commission.field_id = 'porc_comision'
left join tmp_customfieldvalue tc_cobertura on tc_cobertura."ENCODEDKEY" = tl."ENCODEDKEY" and tc_cobertura.field_id = 'porc_cobertura'
left join tmp_customfieldvalue tc_gaurantee on tc_gaurantee."ENCODEDKEY" = tl."ENCODEDKEY" and tc_gaurantee.field_id = 'numero_garantia'
left join tmp_customfieldvalue tc_fetcha on tc_fetcha."ENCODEDKEY" = tl."ENCODEDKEY" and tc_fetcha.field_id = 'fecha_registro_garantia'
left join tmp_customfieldvalue tc_reserve on tc_reserve."ENCODEDKEY" = tl."ENCODEDKEY" and tc_reserve.field_id = 'fecha_reserva'
left join tmp_customfieldvalue tc_cancel on tc_cancel."ENCODEDKEY" = tl."ENCODEDKEY" and tc_cancel.field_id = 'fecha_cancelacion'
left join tmp_customfieldvalue tc_estado on tc_estado."ENCODEDKEY" = tl."ENCODEDKEY" and tc_estado.field_id = 'estado_garantia'
left join m_code_value mc_estado on mc_estado.code_value = tc_estado.field_value and mc_estado.code_id = (select id from m_code where code_name = 'Estado Garantía' )
left join tmp_customfieldvalue tc_obs on tc_obs."ENCODEDKEY" = tl."ENCODEDKEY" and tc_obs.field_id = 'obs_estado_garantia'
left join tmp_customfieldvalue tc_numero on tc_numero."ENCODEDKEY" = tl."ENCODEDKEY" and tc_numero.field_id = 'numero_pagare'
order by ml.id;

truncate table "Informacion Adicional";

INSERT INTO public."Informacion Adicional"
(loan_id, codigo_promotor, nombre_aliado, numero_identificacion_aliado, ciudad_aliado, verificacion_riesgo, tipo_identificacion,
numero_identificacion, dias_mora_ini_mes, fecha_registro_dias_mora_ini_mes, validacion_manual,
fecha_primer_uso, codigo_promotor_original, nombre_promotor, ciudad_cliente, notificacion_bienvenida, departamento_cliente, monto_disponible,
fullname_referer, modelo_externo, created_at, updated_at)
select distinct
 ml.id, tc_promotor.field_value, tc_nombre_aliado.field_value, tc_numero_identificacion_aliado.field_value, tc_ciudad_aliado.field_value, tc_verificacion_riesgo.field_value, tc_tipo_identificacion.field_value,
 tc_numero_identificacion.field_value, tc_dias_mora_ini_mes.field_value, tc_fecha_registro_dias_mora_ini_mes.field_value,
 case 
	when tc_validacion_manual.field_value = 'Si' then true
	else false
end manual,
tc_fecha_primer_uso.field_value, tc_codigo_promotor_original.field_value, tc_nombre_promotor.field_value , tc_ciudad_cliente.field_value, 
case 
	when tc_notificacion_bienvenida.field_value = 'Si' then true
	else false
end bienvenida, tc_departamento_cliente.field_value, 
case 
	when tc_monto_disponible.field_value = 'Si' then true
	else false
end monto_disponible,
tc_fullname_referer.field_value, tc_modelo_externo.field_value, now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_promotor on tc_promotor."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_promotor.field_id) = 'codigo_promotor'
left join tmp_customfieldvalue tc_nombre_aliado on tc_nombre_aliado."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_nombre_aliado.field_id) = 'nombre_aliado'
left join tmp_customfieldvalue tc_numero_identificacion_aliado on tc_numero_identificacion_aliado."ENCODEDKEY" = tl."ENCODEDKEY" and tc_numero_identificacion_aliado.field_id = 'Número_Identificación_Aliado'
left join tmp_customfieldvalue tc_ciudad_aliado on tc_ciudad_aliado."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_ciudad_aliado.field_id) = 'ciudad_aliado'
left join tmp_customfieldvalue tc_verificacion_riesgo on tc_verificacion_riesgo."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_verificacion_riesgo.field_id) = 'verificacion_riesgo'
left join tmp_customfieldvalue tc_tipo_identificacion on tc_tipo_identificacion."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_tipo_identificacion.field_id) = 'tipo_identificacion'
left join tmp_customfieldvalue tc_numero_identificacion on tc_numero_identificacion."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_numero_identificacion.field_id) = 'numero_identificacion'
left join tmp_customfieldvalue tc_dias_mora_ini_mes on tc_dias_mora_ini_mes."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_dias_mora_ini_mes.field_id) = 'dias_mora_ini_mes'
left join tmp_customfieldvalue tc_fecha_registro_dias_mora_ini_mes on tc_fecha_registro_dias_mora_ini_mes."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_fecha_registro_dias_mora_ini_mes.field_id) = 'fecha_registro_dias_mora_ini_mes'
left join tmp_customfieldvalue tc_validacion_manual on tc_validacion_manual."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_validacion_manual.field_id) = 'validacion_manual'
left join tmp_customfieldvalue tc_fecha_primer_uso on tc_fecha_primer_uso."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_fecha_primer_uso.field_id) = 'fecha_primer_uso'
left join tmp_customfieldvalue tc_codigo_promotor_original on tc_codigo_promotor_original."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_codigo_promotor_original.field_id) = 'codigo_promotor_original'
left join tmp_customfieldvalue tc_nombre_promotor on tc_nombre_promotor."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_nombre_promotor.field_id) = 'nombre_promotor'
left join tmp_customfieldvalue tc_ciudad_cliente on tc_ciudad_cliente."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_ciudad_cliente.field_id) = 'ciudad_cliente'
left join tmp_customfieldvalue tc_notificacion_bienvenida on tc_notificacion_bienvenida."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_notificacion_bienvenida.field_id) = 'notificacion_bienvenida'
left join tmp_customfieldvalue tc_departamento_cliente on tc_departamento_cliente."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_departamento_cliente.field_id) = 'departamento_cliente'
left join tmp_customfieldvalue tc_monto_disponible on tc_monto_disponible."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_monto_disponible.field_id) = 'monto_disponible'
left join tmp_customfieldvalue tc_fullname_referer on tc_fullname_referer."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_fullname_referer.field_id) = 'fullname_referer'
left join tmp_customfieldvalue tc_modelo_externo on tc_modelo_externo."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_modelo_externo.field_id) = 'modelo_externo'
order by ml.id;

truncate table "Alianzas";

INSERT INTO public."Alianzas"
(loan_id, estado, valor_real, pendiente_conciliacion, fecha_creacion, valor_real_aprobado, store_alliance, 
sticker_note, value_buy, commission_alliance, percentage_commission, tax_commission_alliance, provider_order_id, created_at, updated_at, loan_transaction_id)
select distinct
 ml.id, tc_estado.field_value, tc_valor.field_value, 
 case 
	when tc_pendient.field_value = 'Si' then true
	else false
end req,
to_date(tc_created.field_value, 'YYYY-MM-DD'), tc_real.field_value::numeric, tc_store.field_value,
 tc_sticker.field_value, tc_buy.field_value, tc_alliance.field_value, tc_percetage.field_value, tc_tax.field_value,  tc_provider.field_value, now(), now(), null
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_estado on tc_estado."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_estado.field_id) = 'estado'
left join tmp_customfieldvalue tc_valor on tc_valor."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_valor.field_id) = 'valor_real'
left join tmp_customfieldvalue tc_pendient on tc_pendient."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_pendient.field_id) = 'pendiente_conciliacion'
left join tmp_customfieldvalue tc_created on tc_created."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_created.field_id) = 'fecha_creacion'
left join tmp_customfieldvalue tc_real on tc_real."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_real.field_id) = 'valor_real_aprobado'
left join tmp_customfieldvalue tc_store on tc_store."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_store.field_id) = 'store_alliance'
left join tmp_customfieldvalue tc_sticker on tc_sticker."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_sticker.field_id) = 'sticker_note'
left join tmp_customfieldvalue tc_buy on tc_buy."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_buy.field_id) = 'value_buy'
left join tmp_customfieldvalue tc_alliance on tc_alliance."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_alliance.field_id) = 'commission_alliance'
left join tmp_customfieldvalue tc_percetage on tc_percetage."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_percetage.field_id) = 'percentage_commission'
left join tmp_customfieldvalue tc_tax on tc_tax."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_tax.field_id) = 'tax_commission_alliance'
left join tmp_customfieldvalue tc_provider on tc_provider."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_provider.field_id) = 'provider_order_id';


truncate table "Informacion Financiera";

INSERT INTO public."Informacion Financiera"
(loan_id, ingresos, activos, created_at, updated_at)
select distinct
 ml.id, tc_ingreso.field_value::numeric, tc_activo.field_value::numeric, now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_ingreso on tc_ingreso."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_ingreso.field_id) = 'ingresos'
left join tmp_customfieldvalue tc_activo on tc_activo."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_activo.field_id) = 'activos';


truncate table "Informacion Bancaria";

INSERT INTO public."Informacion Bancaria"
(loan_id, entidad_financiera, tipo_de_cuenta, numero_de_cuenta, created_at, updated_at)
select distinct
 ml.id, tc_entidad.field_value, tc_tipo.field_value, tc_cuenta.field_value , now(), now()
from 
m_loan ml
join tmp_loanaccount tl on tl."ID" = ml.external_id
left join tmp_customfieldvalue tc_entidad on tc_entidad."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_entidad.field_id) = 'entidad_financiera'
left join tmp_customfieldvalue tc_tipo on tc_tipo."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_tipo.field_id) = 'tipo_de_cuenta'
left join tmp_customfieldvalue tc_cuenta on tc_cuenta."ENCODEDKEY" = tl."ENCODEDKEY" and lower(tc_cuenta.field_id) = 'numero_de_cuenta';


-- ----- End Custom Fields ---------------------



-- Create savings accounts for clients
INSERT INTO public.m_savings_account
(account_no, client_id, product_id, status_enum, sub_status_enum, account_type_enum, deposit_type_enum, submittedon_date,
submittedon_userid, approvedon_date, approvedon_userid, activatedon_date, activatedon_userid, currency_code,
currency_digits, currency_multiplesof, nominal_annual_interest_rate, interest_compounding_period_enum, interest_posting_period_enum, interest_calculation_type_enum,
interest_calculation_days_in_year_type_enum, withdrawal_fee_for_transfer, allow_overdraft, overdraft_limit, nominal_annual_interest_rate_overdraft,
min_overdraft_for_interest_calculation, min_required_balance, enforce_min_required_balance, max_allowed_lien_limit, created_by, last_modified_by, created_on_utc, last_modified_on_utc)
select 
mc.id account_no, mc.id client_id, 1 product_id, 300 status_enum, 0 sub_status_enum, 1 account_type_enum, 100 deposit_type_enum, mc.activation_date submittedon_date,
1 submittedon_userid, mc.activation_date approvedon_date, 1 approvedon_userid, mc.activation_date activatedon_date, 1 activatedon_userid,  'COP'currency_code,
2 currency_digits, 1 currency_multiplesof, 0.000000 nominal_annual_interest_rate, 1 interest_compounding_period_enum, 4 interest_posting_period_enum, 1 interest_calculation_type_enum,
365 interest_calculation_days_in_year_type_enum, false withdrawal_fee_for_transfer, false allow_overdraft, 0.000000 overdraft_limit, 0.000000 nominal_annual_interest_rate_overdraft,
0.000000 min_overdraft_for_interest_calculation, 0 min_required_balance, false enforce_min_required_balance, 0 max_allowed_lien_limit,  1 created_by, 1 last_modified_by,
now(), now()
from 
m_client mc;

update m_savings_account set account_no = LPAD(id::text, 9, '0') ;

-- update submit and approved dates

update m_loan mlt set approvedon_date = sub.approved_date, submittedon_date = sub.submit_date
from (
select ml.id, tld.submit_date, tld.approved_date
from tmp_loan_dates tld
join tmp_loanaccount lt on lt."ENCODEDKEY" = tld."ENCODEDKEY"
join m_loan ml on ml.external_id = lt."ID"
) sub
where mlt.id = sub.id;

-- update configuration penalty-start-date  set it to tomorrow's date

-- Execute Loan Arrears Aging Job

update m_charge set name = tmp_name;


-- After migration multi disbursal loans execute below queries
update m_loan set account_no = account_no || '-old';
update m_loan set account_no = m_loan.external_id;

select count(*) from m_loan ml where loan_status_id = 300;


----- TESTING SCRIPTS BELOW -- NOT PART OF MIGRATION------

select * from m_charge order by id;

select * from m_loan ml where product_id = 10;

select * from tmp_loanaccount tcm where tcm."ID" not in (select external_id from m_loan) and tcm."ENCODEDKEY" = '8a44431096d490d00196d58884ed0453';

select * from tmp_disbursementdetails td where "ENCODEDKEY" = '8a44431096d490d00196d58884ed0454';

select * from tmp_loanaccount tcm 
join tmp_disbursementdetails td on tcm."DISBURSEMENTDETAILSKEY" = td."ENCODEDKEY" 
join tmp_principalpaymentaccountsettings ppas on ppas."ENCODEDKEY" = tcm."PRINCIPALPAYMENTSETTINGSKEY"
join tmp_loanproduct_mapping tlm on tlm.ea_product_key = tcm."PRODUCTTYPEKEY"
join m_product_loan mpl on mpl.name = tlm.mifos_product_name
join tmp_cliente_migrar tcm2 on tcm2.encodedkey = tcm."ACCOUNTHOLDERKEY"
join m_client mc on mc.external_id = tcm2.ID
join campos_cliente_persona ccp on ccp.client_id = mc.id
where tcm."ID" not in (select external_id from m_loan)

select * from m_product_loan;

select count(*) from m_loan where product_id > 11;

select * from m_loan ml where external_id = '2759447517';

select * from m_loan_repayment_schedule mlrs where loan_id = 1381 order by installment;

select * from m_loan_charge mlc where mlc.loan_id = 1381 order by id;

select * from tmp_migration_response where activity = 'disburse';

select * from tmp_loanproduct_mapping tlm join m_product_loan mpl on mpl.name = tlm.mifos_product_name;

select * from tmp_loanaccount tl where tl."ID" = '1522804147'; --0634643832, 5004368732, 5004368732, 0229761176, 1203523376, 1522804147, 9384153626, 9308949369


select * from m_loan_charge mlc where id = 31568;
select * from m_charge mc where id = 36;
select * from m_charge mc order by id;
select "CREATIONDATE", "ENTRYDATE", "TYPE", "AMOUNT", * from tmp_loantransaction tl where tl."PARENTACCOUNTKEY" = '8a445323883425e70188344edd5110fa' order by "ENTRYDATE";

select * from tmp_loan_charges tlc where tlc.loankey = '8a445323883425e70188344edd5110fa'

select "ENCODEDKEY" from tmp_loanaccount tl where "ID" in ('0634643832', '5004368732', '5004368732', '0229761176', '1203523376', '1522804147');

select * from m_loan order by id;

select * from m_client mc where id in (select client_id from m_loan);

SELECT mc.id AS clientId,
                cce."NIT" AS nit,
                tipo.code_value AS tipo,
                ccp."Cedula" AS cedula
                FROM m_client mc
                LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                LEFT JOIN m_code_value tipo ON tipo.id = cce."Tipo ID_cd_Tipo ID"
                LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                WHERE cce."NIT" = '88217953' OR ccp."Cedula" = '88217953';
               
               

select * from m_charge order by id;


