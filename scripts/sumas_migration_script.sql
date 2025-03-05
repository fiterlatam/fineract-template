-- COPY LOAN PRODUCT DATA
-- COPY CODE AND CODE VALUES
-- below query in case this column is missing

alter table m_payment_detail add column point_of_sales_code varchar(6) null;


Review pods and logs
********************
Execute the following command
ssh luisdp@10.66.157.23
/ then enter the following pass CoreSumas2024*
Enter the following command to get the pod ID
k get po -n fineract-qa
Enter the following command to get the logs of the given pod
k logs -f fineract-6486846fcc-95c6x -n fineract-qa



-- TRUNCATE SCRIPT----------


-- If truncate command is stuck then execute below command
SELECT
    activity.pid,
    activity.usename,
    activity.query,
    blocking.pid AS blocking_id,
    blocking.query AS blocking_query
FROM pg_stat_activity AS activity
JOIN pg_stat_activity AS blocking ON blocking.pid = ANY(pg_blocking_pids(activity.pid));


SELECT pg_terminate_backend(blocking_id);

-----------------------------------------------------

truncate table m_client cascade;

truncate table campos_cliente_empresas;

truncate table custom.c_client_ally_point_of_sales cascade;

truncate table custom.c_client_ally cascade;

truncate table m_loan cascade;

#Try to restart the pods and if they fail then execute these scripts to get the migrations to pass
alter table m_loan_transaction drop column is_partially_ivoiced;
alter table m_loan_transaction drop column is_invoiced_generated_by_job;

-- Disable the following Jobs Before Migration --
-- Arrears Job
-- Penalties Job
-- Daily Accruals Job
-- Daily Charge Accruals Job

-- product start date needs to be moved back
-- client activation date needs to be moved back

Validation Scripts:

----------CLIENTS-----------------------
-- Client Mobile Number Duplication

select * from tmp_clientes_migrar where mobile_number in (
	select mobile_number from tmp_clientes_migrar c group by mobile_number having count(mobile_number)  > 1)
	order by mobile_number;

-- Null Gender

select * from tmp_clientes_migrar where gender is null;

-- Missing city
select * from tmp_empresa_migrar where city_id not in (select code_score from m_code_value where code_id = (select id from m_code where code_name = 'Ciudad'));

-- Null referencia
select * from tmp_clientes_migrar where celular_referencia is null;

select * from tmp_clientes_migrar where referencia is null;

------------------------------------------

-- Allies------------
select * from tmp_empresa_migrar where upper(liquidation_frequency_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'FrecuenciaLiquidacion'));
select * from tmp_empresa_migrar where department_id  not in (select code_score from m_code_value where code_id = (select id from m_code where code_name = 'Departamento'));
select * from tmp_empresa_migrar where city_id not in (select code_score from m_code_value where code_id = (select id from m_code where code_name = 'Ciudad'));
select * from tmp_empresa_migrar where tax_profile_id is null;
select * from tmp_empresa_migrar where state_id is null;


select * from tmp_empresa_migrar where upper(account_type_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'TipoCuentaBancaria'));

select * from tmp_empresa_migrar where upper(bank_entity_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'EntidadBancaria'));

select * from tmp_empresa_migrar where upper(tax_profile_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'PerfilTributarioRegimenIVA'));

select * from tmp_empresa_migrar where upper(state_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'Estado'));

-- Check for duplicate NIT
select * from tmp_empresa_migrar where nit in (select nit from tmp_empresa_migrar cca group by nit having count(nit) > 1)

-- -------------------------------------------------------------

------------POINT OF SALES------------------

select * from tmp_puntoscredito_migrar where upper(brand_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'Marca')) 
order by brand_id;

select * from tmp_puntoscredito_migrar where city_id  not in (select code_score from m_code_value where code_id = (select id from m_code where code_name = 'Ciudad'));

select * from tmp_puntoscredito_migrar where department_id  not in (select code_score from m_code_value where code_id = (select id from m_code where code_name = 'Departamento'));

select * from tmp_puntoscredito_migrar where upper(category_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'CategoriaPuntoDeVenta'));
select * from tmp_puntoscredito_migrar where upper(segment_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'SegmentoPuntoDeVenta'));
select * from tmp_puntoscredito_migrar where upper(type_id)  not in (select upper(code_value) from m_code_value where code_id = (select id from m_code where code_name = 'TipoPuntoDeVenta'));

select * from tmp_puntoscredito_migrar where client_ally_id not in (select nit from tmp_empresa_migrar);

-- check for duplicate POS
select count(id), code, "name"
from (
select
cca.id ,
tpm.code,
tpm."name" ,
brand_value.id brand_id,
city_value.id city_id,
department_value.id department_id,
category_value.id category_id,
segment_value.id segment_id,
type_value.id type_id,
tpm.settled_comission::double precision ,
tpm.buy_enabled ,
tpm.collection_enabled,
(select id from m_code_value where code_id = (select id from m_code where code_name ='Estado') and upper(code_value) = tpm.state_id) state_id
from
tmp_puntoscredito_migrar tpm
join custom.c_client_ally cca on cca.nit = tpm.client_ally_id
join m_code_value brand_value on upper(brand_value.code_value) = upper(tpm.brand_id) and brand_value.code_id = (select id from m_code where code_name ='Marca')
join m_code_value city_value on city_value.code_score = tpm.city_id and city_value.code_id = (select id from m_code where code_name ='Ciudad')
join m_code_value department_value on department_value.code_score = tpm.department_id and department_value.code_id = (select id from m_code where code_name ='Departamento')
join m_code_value category_value on upper(category_value.code_value) = upper(tpm.category_id) and category_value.code_id = (select id from m_code where code_name ='CategoriaPuntoDeVenta')
join m_code_value segment_value on upper(segment_value.code_value) = upper(tpm.segment_id) and segment_value.code_id = (select id from m_code where code_name ='SegmentoPuntoDeVenta')
join m_code_value type_value on upper(type_value.code_value) = upper(tpm.type_id) and type_value.code_id = (select id from m_code where code_name ='TipoPuntoDeVenta')
) x group by code, "name"
having count(id) > 1



------------------------------------------------------------------

------------------LOAN ACCOUNTS-----------------------------------
-- Check for multiple loan products in a single loan

with cte as (select
nit , code , cre_numerocredito , cli_nroid , min(cuo_nrocuota) min_inst, cre_producto_id
from 
tmp_creditos_migrar tcm
group by nit, code, cre_numerocredito, cli_nroid, cre_producto_id
)
select * from cte where min_inst > 1

-- Check for duplicate external_ids

SELECT * FROM tmp_creditos_migrar WHERE external_id IN (SELECT external_id FROM tmp_creditos_migrar where cuo_nrocuota = 1 GROUP BY external_id HAVING COUNT(external_id) > 1)
ORDER BY external_id

-- Check for mismatch in interest rates
select * from tmp_creditos_migrar tcm where tcm.cuo_porinteres != tcm.cpc_porcentaje_interes

-- Check for missing nit and code
select * from tmp_creditos_migrar tcm where tcm.cpc_monto_aval is not null and (tcm.nit is null or tcm.code is null);

-- Check for future submit date
select * from tmp_creditos_migrar tcm where tcm.cre_fechafinancia > current_date;


---------------------------------------------------------------------------------------------

----------------END VALIDATION SCRIPTS--------------------------------------

-- Individual Client Insert



update tmp_clientes_migrar set middle_name = null where  length(trim(middle_name)) = 0;

insert into m_client (account_no, external_id, status_enum, submittedon_date, activation_date, office_joining_date, office_id, firstname, middlename, lastname, second_lastname, display_name,
						mobile_no, gender_cv_id, date_of_birth, legal_form_enum, email_address, created_on_utc, last_modified_on_utc,  activatedon_userid, created_by, last_modified_by )
select 
	c.external_id as account_no,
	c.external_id as external_id, 
	300 as status_enum,
	current_date as submittedon_date,
	current_date as activation_date,
	current_date as office_joining_date,
	1 as office_id,
	trim(c.first_name) as firstname,
	coalesce(c.middle_name, null) as middlename,
	c.last_name as lastname,
	c.last_name_two as second_lastname,
	c.first_name || case when c.middle_name is null then ' ' else ' '  || c.middle_name || ' ' end || c.last_name as display_name,
	c.mobile_number as mobile_no,
	CASE 
		when trim(c.gender) = 'F' then (select id from m_code_value where code_value='Mujer' and code_id = (select id from m_code where code_name='Gender'))
		when trim(c.gender) = 'M' then (select id from m_code_value where code_value='Hombre' and code_id = (select id from m_code where code_name='Gender'))
		ELSE null
	END as gender_cv_id,
	c.date_of_birth as date_of_birth,
	1 as legal_form_enum,
	c.emailaddress as email_address,
	current_date as created_on_utc,
	current_date as last_modified_on_utc,
	1 as activatedon_userid, 
	1 as created_by, 
	1 as last_modified_by
	from
	tmp_clientes_migrar c;

-- Individual Client campos data Insert
-- Below code_value insert queries will probably be not needed for production because before starting production migration code values from uat should be fetched and inserted in production

-- Update the sequence on m_code_value table so that the next inserts can work
SELECT setval('m_code_value_id_seq',(SELECT GREATEST(MAX(id), nextval('m_code_value_id_seq')-1) FROM m_code_value));

insert into m_code_value (code_id, code_value, order_position, code_score, is_active, is_mandatory)
values (
 (select id from m_code where code_name ='Parentesco'), 'OTRO', 28, 28, true, false
);

insert into m_code_value (code_id, code_value, order_position, code_score, is_active, is_mandatory)
values (
 (select id from m_code where code_name ='Parentesco'), 'TBD-35', 35, 35, true, false
);

-- Takes about 3-4 minutes
INSERT INTO campos_cliente_persona
(client_id, "Cedula", "Estado Civil_cd_Estado civil", "Edad", "Estrato", "Ciudad_cd_Ciudad", "Tiene vehiculo propio", "Tipo Vehiculo_cd_Tipo de vehiculo", "Nivel Academico_cd_Nivel academico", 
"Actividad Laboral_cd_Actividad laboral", "Tiempo de actividad laboral", "Media de ingresos", "Nombre empresa", "Direccion", "Telefono", "Cupo solicitado", "Cupo aprobado", "Cupo score", 
"Valor score", "Modelo score", "Referencia", "Celular Referencia", "Parentesco_cd_Parentesco", created_at, updated_at, "Cupo otros prestamos")
select 
	mc.id as client_id,
	c.id as "Cedula",
	marital_status.id as "Estado Civil_cd_Estado civil",
	c.age as "Edad",
	c.estrato as "Estrato",
	city_value.id as "Ciudad_cd_Ciudad",
	case 
		when c.has_own_vehicle = 'N' then false
		else true
	end	as "Tiene vehiculo propio",
	vehicle_value.id as "Tipo Vehiculo_cd_Tipo de vehiculo",
	academic_value.id as "Nivel Academico_cd_Nivel academico",
	workActivity_value.id as "Actividad Laboral_cd_Actividad laboral",
	c.work_time as "Tiempo de actividad laboral",
	c.average_income as "Media de ingresos",
	c.company_name as "Nombre empresa",
	c.address as "Direccion",
	c.phone as "Telefono",
	c.cupo_requested as "Cupo solicitado",
	c.cupo_approved as "Cupo aprobado",
	c.cupo_score as "Cupo score",
	c.score_value as "Valor score",
	c.modelo_score as "Modelo score",
	c.referencia as "Referencia",
	c.celular_referencia as "Celular Referencia",
	relationship_value.id as "Parentesco_cd_Parentesco",
	current_date as "created_at",
	current_date as "updated_at",
	c.cupo_otros_prestamos
from 
tmp_clientes_migrar c
join m_client mc on mc.external_id = c.external_id::varchar 
 join m_code_value marital_status on marital_status.code_score = c.marital_status::varchar
 join m_code marital_code on marital_code.id = marital_status.code_id and marital_code.code_name ='Estado Civil'
 join m_code_value city_value on city_value.code_score = c.city
 join m_code city_code on city_code.id = city_value.code_id and city_code.code_name ='Ciudad'
  left join m_code_value vehicle_value on vehicle_value.code_score = c.type_of_vehicle::varchar and vehicle_value.code_id = (select id from m_code where code_name ='Tipo Vehiculo')
 left join m_code_value academic_value on academic_value.code_score = c.academic_level::varchar and academic_value.code_id = (select id from m_code where code_name ='Nivel Academico')
 left join m_code_value workActivity_value on workActivity_value.code_score = c.work_activity::varchar and workActivity_value.code_id = (select id from m_code where code_name ='Actividad Laboral')
 left join m_code_value relationship_value on relationship_value.code_score = c.parentesco::varchar and relationship_value.code_id = (select id from m_code where code_name ='Parentesco');



---------------------- ALLIES--------------------
update m_code_value set code_value = 'mensual' where code_value = 'mensual ';
update m_code_value set code_value = 'Semanal' where code_value = 'Semanal ';
update m_code_value set code_score = '05' where code_score = '5' and code_id = (select id from m_code where code_name = 'Departamento');
update m_code_value set code_score = '08' where code_score = '8' and code_id = (select id from m_code where code_name = 'Departamento');

update tmp_empresa_migrar set settled_comission = replace(settled_comission,',', '.');

INSERT INTO custom.c_client_ally
(company_name, nit, nit_digit, address, city_id, department_id, liquidation_frequency_id, apply_cupo_max_sell, cupo_max_sell, settled_comission, buy_enabled, collection_enabled,
bank_entity_id, account_type_id, account_number, tax_profile_id, state_id)
select
tem.company_name,
tem.nit,
tem.nit_digit ,
tem.address ,
city_value.id as city_id,
department_value.id as department_id,
liquidation_value.id as liquidation_frequency_id,
tem.apply_cupo_max_sell,
tem.cupo_max_sell::double PRECISION,
tem.settled_comission::double PRECISION ,
tem.buy_enabled ,
tem.collection_enabled ,
bank_value.id as bank_entity_id ,
accounttype_value.id as account_type_id,
tem.account_number ,
tax_value.id as tax_profile_id ,
state_value.id as state_id 
from 
	tmp_empresa_migrar tem
	join m_code_value city_value on city_value.code_score = tem.city_id and city_value.code_id = (select id from m_code where code_name ='Ciudad')
	join m_code_value department_value on department_value.code_score = tem.department_id and department_value.code_id = (select id from m_code where code_name ='Departamento')
	join m_code_value liquidation_value on upper(liquidation_value.code_value) = upper(tem.liquidation_frequency_id) and liquidation_value.code_id = (select id from m_code where code_name ='FrecuenciaLiquidacion')
	join m_code_value tax_value on upper(tax_value.code_value) = upper(tem.tax_profile_id) and tax_value.code_id = (select id from m_code where code_name ='PerfilTributarioRegimenIVA')
	join m_code_value state_value on upper(state_value.code_value) = upper(tem.state_id) and state_value.code_id = (select id from m_code where code_name ='Estado')
	left join m_code_value accounttype_value on upper(accounttype_value.code_value) = upper(tem.account_type_id) and accounttype_value.code_id = (select id from m_code where code_name ='TipoCuentaBancaria')
	left join m_code_value bank_value on upper(bank_value.code_value) = upper(tem.bank_entity_id) and bank_value.code_id = (select id from m_code where code_name ='EntidadBancaria');
	


--------------------------------------POINT OF SALES--------------------------------------------


update tmp_puntoscredito_migrar set settled_comission = replace(settled_comission,',', '.');

INSERT INTO custom.c_client_ally_point_of_sales
(client_ally_id, code, "name", brand_id, city_id, department_id, category_id, segment_id, type_id, settled_comission, buy_enabled, collection_enabled, state_id)
select 
cca.id ,
tpm.code,
tpm."name" ,
brand_value.id brand_id,
city_value.id city_id,
department_value.id department_id,
category_value.id category_id,
segment_value.id segment_id,
type_value.id type_id,
tpm.settled_comission::double precision ,
tpm.buy_enabled ,
tpm.collection_enabled,
(select id from m_code_value where code_id = (select id from m_code where code_name ='Estado') and upper(code_value) = tpm.state_id) state_id
from
tmp_puntoscredito_migrar tpm
join custom.c_client_ally cca on cca.nit = tpm.client_ally_id 
join m_code_value brand_value on upper(brand_value.code_value) = upper(tpm.brand_id) and brand_value.code_id = (select id from m_code where code_name ='Marca')
join m_code_value city_value on city_value.code_score = tpm.city_id and city_value.code_id = (select id from m_code where code_name ='Ciudad')
join m_code_value department_value on department_value.code_score = tpm.department_id and department_value.code_id = (select id from m_code where code_name ='Departamento')
join m_code_value category_value on upper(category_value.code_value) = upper(tpm.category_id) and category_value.code_id = (select id from m_code where code_name ='CategoriaPuntoDeVenta')
join m_code_value segment_value on upper(segment_value.code_value) = upper(tpm.segment_id) and segment_value.code_id = (select id from m_code where code_name ='SegmentoPuntoDeVenta')
join m_code_value type_value on upper(type_value.code_value) = upper(tpm.type_id) and type_value.code_id = (select id from m_code where code_name ='TipoPuntoDeVenta')
-- join m_code_value state_value on upper(state_value.code_value) = upper(tpm.state_id) and type_value.code_id = (select id from m_code where code_name ='Estado')



---------------------------------------Loan Accounts----------------------------------
-- WHEN DOING THE FILE IMPORT, MAKE SURE SPANISH LANGUAGE IS SELECTED ON THE WEB APP OTHERWISE LOANS WILL FAIL
-- Aval Charge

INSERT INTO m_charge
("name", currency_code, charge_applies_to_enum, charge_time_enum, charge_calculation_enum, charge_payment_mode_enum, amount, fee_on_day, fee_interval, fee_on_month, is_penalty, is_active, is_deleted, min_cap, max_cap, fee_frequency, is_free_withdrawal, free_withdrawal_charge_frequency, restart_frequency, restart_frequency_enum, is_payment_type, payment_type_id, income_or_liability_account_id, tax_group_id, grace_on_charge_period_enum, grace_on_charge_period_amount, parent_charge_id, interest_rate_id, insurance_name, insurance_charged_as, insurance_company, insurer_name, insurance_code, insurance_plan, base_value, vat_value, total_value, deadline, is_get_percentage_from_table, days_in_arrears)
VALUES('Aval Migrar', 'COP', 1, 8, 286, 0, 500.000000, NULL, NULL, NULL, false, true, false, NULL, NULL, NULL, false, 0, 0, 0, false, NULL, NULL, NULL, 1, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, NULL);
INSERT INTO m_charge
("name", currency_code, charge_applies_to_enum, charge_time_enum, charge_calculation_enum, charge_payment_mode_enum, amount, fee_on_day, fee_interval, fee_on_month, is_penalty, is_active, is_deleted, min_cap, max_cap, fee_frequency, is_free_withdrawal, free_withdrawal_charge_frequency, restart_frequency, restart_frequency_enum, is_payment_type, payment_type_id, income_or_liability_account_id, tax_group_id, grace_on_charge_period_enum, grace_on_charge_period_amount, parent_charge_id, interest_rate_id, insurance_name, insurance_charged_as, insurance_company, insurer_name, insurance_code, insurance_plan, base_value, vat_value, total_value, deadline, is_get_percentage_from_table, days_in_arrears)
VALUES('iva aval migrar', 'COP', 1, 8, 342, 0, 19.000000, NULL, NULL, NULL, false, true, false, NULL, NULL, NULL, false, NULL, NULL, NULL, false, NULL, NULL, NULL, 1, 0, (select id from m_charge where "name" = 'Aval Migrar'), NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, NULL);

---- Update client activation and submission date to before 2022
update m_client set activation_date = '2021-01-01', submittedon_date = '2021-01-01', office_joining_date = '2021-01-01';

---- update product interest rate
update m_product_loan set min_nominal_interest_rate_per_period = 0, max_nominal_interest_rate_per_period = 40 where id in (1,2,3, 4)

--- update product start date
update m_product_loan set start_date = '2021-01-01' where id in (1,2,3, 4)

-- Update cupo amount to high value so that loans can be processed. Revert this limit back to the original limit
update campos_cliente_persona set "Cupo solicitado" = 20000000, "Cupo aprobado" = 20000000;

-- Update is_advance field on m_product_loan id 3 to allow loans to disburse
update m_product_loan set is_advance = false where id = 3; -- in (3,9); --for production


--- Indexes to speed up retrieval of records --
 CREATE INDEX idx_cedula
ON tmp_creditos_migrar(cli_nroid);

 CREATE INDEX idx_cedula_campos_cliente_persona
ON campos_cliente_persona("Cedula");

-- Backup and update the maximum legal rate data --
create table m_maximum_credit_rate_configuration_bk as select * from m_maximum_credit_rate_configuration;

select * from m_maximum_credit_rate_configuration_bk;

update m_maximum_credit_rate_configuration
set annual_nominal_rate = 50, monthly_nominal_rate = 10, daily_nominal_rate = 5, overdue_interest_rate = 32.5;

----- Insert offices [PROD] ----
insert into m_office (parent_id, "hierarchy", "name", opening_date)
values(1, '.2.', 'Migration Office 2', '2009-01-01');

insert into m_office (parent_id, "hierarchy", "name", opening_date)
values(1, '.3.', 'Migration Office 3', '2009-01-01');

insert into m_office (parent_id, "hierarchy", "name", opening_date)
values(1, '.4.', 'Migration Office 4', '2009-01-01');

insert into m_office (parent_id, "hierarchy", "name", opening_date)
values(1, '.5.', 'Migration Office 5', '2009-01-01');

insert into m_office (parent_id, "hierarchy", "name", opening_date)
values(1, '.6.', 'Migration Office 6', '2009-01-01');

insert into m_office (parent_id, "hierarchy", "name", opening_date)
values(1, '.7.', 'Migration Office 7', '2009-01-01');

----------------------
-- On uat 15k clients already have been assigned to office 2. So no need to execute this query on uat
-- Took around 7 minutes on uat with 15k rows
-- Make sure office is already created
-- Calculate how many import files you want to create and based on that adjust the number of clients to be allocated to the office. Change office number in update query for each batch
with cte as (
select distinct id from m_client mc
join campos_cliente_persona ccp on ccp.client_id = mc.id
join tmp_creditos_migrar tcm on tcm.cli_nroid = ccp."Cedula"
where mc.office_id = 1
limit 15000
)
update m_client mc
set office_id = 2
from cte 
where 
cte.id = mc.id;

-- Download loan import file for each office and update office id in below query based on the office of import file

select distinct
	(select REPLACE(mo.name,' ', '_') from m_office mo where id = 2) as office_name,
	'Individual' as loan_type,
	concat(trim(mc.display_name), '(', mc.id , ')') as client_name,
	mc.external_id as client_external_id,
	REpLACE(mpl.name, ' ', '_') as product_name,
	'' loan_officer,
	cre_fechafinancia as submit_date,
	cre_fechafinancia as approved_date,
	cre_fechafinancia as disbursement_date,
	'' payment_type,
	'' fund,
	cre_valorcredito as principal_amount,
	cre_nrocuotas as no_of_repayments,
	1 repaid_every,
	'Months' repay_freq,
	cre_nrocuotas loan_term,
	'Months' loan_term_freq,
	cuo_porinteres as nominal_interest_rate,
	'Per Year' as interest_rate_per_month,
	'Equal installments' as ammortization,
	'Declining Balance' as interest_method,
	'Same as repayment period' interest_calculation_period,
	'0' arrears_tolerance,
	'advanced-payment-allocation-strategy' repayment_strategy,
	mpl.grace_on_principal_periods grace_on_principal_pmt,
	mpl.grace_on_interest_periods grace_on_interest_pmt,
	mpl.grace_on_charges_periods interest_free_period,
	'' interest_charged_from, 
	'' first_repayment_on,
	'' amount_repaid,
	'' date_last_repayment,
	'' repayment_type,
	'',
	'',
	'',
	tcm.external_id as external_id,
	case
		when cpc_monto_aval > 0 then 'Aval_Migrar'
		else null
	end charge_name,
	case
		when cpc_monto_aval > 0 then round(cpc_monto_aval/(1+(cpc_porcentaje_iva/100)))
		else
		null
	end charge_amount,
	'',
	case
		when cpc_monto_aval > 0 then 'iva_aval_migrar'
		else
		null
	end vat_charge_name,
	case
		when cpc_monto_aval > 0 then 19
		else
		null
	end vat_charge_amount,
	'',
	nit, code, cre_numerocredito::varchar, cli_nroid, 
	cre_fechafinancia
from 
	tmp_creditos_migrar tcm
	join campos_cliente_persona ccp on ccp."Cedula" = tcm.cli_nroid
	join m_client mc on mc.id = ccp.client_id 
 	join m_product_loan mpl on mpl.id = 
 	case 
 		when tcm.cre_producto_id = 52 then 1
 		when tcm.cre_producto_id = 53 then 2
 		when tcm.cre_producto_id = 54 then 3
 		when tcm.cre_producto_id = 55 then 4
 	end
 	where
 	mc.office_id = 2
 	-- and tcm.nit = '800069933' and code = '2655' and cre_numerocredito = 208 and cli_nroid = '92541184'
	order by tcm.cli_nroid, tcm.cre_fechafinancia
	limit 5000 -- first 5k loans for first sheet
 --	 limit 5000 offset 5000 -- next5k loans for second sheet
 --	 limit 5000 offset 10000 -- next 5k loans for third sheet
 --	 limit 5000 offset 15000 -- next 5k loans for forth sheet
 --	 limit 5000 offset 20000 -- next 5k loans for fifth sheet


-- Set the original cupo value for client
update campos_cliente_persona ccp
set "Cupo aprobado" = tcm.cupo_approved,
"Cupo solicitado" = tcm.cupo_requested
from m_client mc
inner join tmp_clientes_migrar tcm on mc.external_id = cast(tcm.external_id as text)
where ccp.client_id = mc.id

-- Reinstate is_advance field on m_product_loan id 3
update m_product_loan set is_advance = true where id in (3,9);


----------------------------------- Loan Transactions ----------------------------------------------
-- Update repayment schedule paid installments
update m_loan_repayment_schedule mlrs
set principal_completed_derived = mlrs.principal_amount,
	interest_completed_derived = mlrs.interest_amount,
	fee_charges_completed_derived = mlrs.fee_charges_amount,
	penalty_charges_completed_derived = mlrs.penalty_charges_amount,
	migrated_installment = true,
	completed_derived = true,
	obligations_met_on_date = tcm.cpc_fecha_pago_cuota
from m_loan ml
inner join tmp_creditos_migrar tcm on ml.external_id = tcm.external_id
where ml.id = mlrs.loan_id
and mlrs.installment = tcm.cuo_nrocuota
and tcm.cpc_fecha_pago_cuota is not null;

-- Validate that all paid installments are migrated as paid; this query should return no results
-- If it does, treat them accordingly, or re-run the preceeding query.
-- Don't proceed to the next query until this returns zero records
select tcm.cpc_fecha_pago_cuota, mlrs.* from m_loan ml join m_loan_repayment_schedule mlrs
on ml.id = mlrs.loan_id
join tmp_creditos_migrar tcm on ml.external_id = tcm.external_id
where mlrs.installment = tcm.cuo_nrocuota
and tcm.cpc_fecha_pago_cuota is not null
and mlrs.completed_derived = false
order by mlrs.installment;

-- Alter loan_transaction add installment_id (we'll drop this after)
alter table m_loan_transaction add column installment_id bigint;

-- Insert transactions for completed installments
INSERT INTO m_loan_transaction(
	loan_id, office_id, payment_detail_id, is_reversed, external_id, installment_id, transaction_type_enum, transaction_date, amount, principal_portion_derived, interest_portion_derived, fee_charges_portion_derived, penalty_charges_portion_derived, outstanding_loan_balance_derived, submitted_on_date, created_by, last_modified_by, created_on_utc, last_modified_on_utc)
select ml.id, mc.office_id, null, false, null, mlrs.id, 2, mlrs.obligations_met_on_date, coalesce(mlrs.principal_amount, 0) + coalesce(mlrs.interest_amount,0) + coalesce(mlrs.fee_charges_amount, 0) + coalesce(mlrs.penalty_charges_amount, 0),
mlrs.principal_amount, mlrs.interest_amount, mlrs.fee_charges_amount, mlrs.penalty_charges_amount, null, mlrs.obligations_met_on_date, 1, 1, mlrs.obligations_met_on_date, mlrs.obligations_met_on_date
from m_loan_repayment_schedule mlrs join m_loan ml on mlrs.loan_id = ml.id
join m_client mc on ml.client_id = mc.id
where mlrs.completed_derived = true
and mlrs.installment > 0
order by mlrs.installment;

-- Insert transaction to schedule mapping
INSERT INTO m_loan_transaction_repayment_schedule_mapping(
	loan_transaction_id, loan_repayment_schedule_id, amount, principal_portion_derived, interest_portion_derived, fee_charges_portion_derived, penalty_charges_portion_derived)
select mlt.id, mlrs.id, mlt.amount, mlt.principal_portion_derived, mlt.interest_portion_derived, mlt.fee_charges_portion_derived, mlt.penalty_charges_portion_derived
from m_loan_repayment_schedule mlrs join m_loan_transaction mlt on mlrs.id = mlt.installment_id
where mlrs.completed_derived = true and mlrs.installment > 0


-- Update loan balance in transactions (took about 7 minutes for 25K loans; took 27 minutes for 200K loans)
UPDATE m_loan_transaction lt
SET outstanding_loan_balance_derived = (
    SELECT ml.principal_disbursed_derived - COALESCE(SUM(lt2.principal_portion_derived), 0)
    FROM m_loan ml
    LEFT JOIN m_loan_transaction lt2 ON lt2.loan_id = ml.id
    where ml.id = lt.loan_id and lt2.transaction_date <= lt.transaction_date
    and lt2.transaction_type_enum = 2
    group by ml.principal_disbursed_derived
)
where lt.outstanding_loan_balance_derived IS DISTINCT FROM (
    SELECT ml.principal_disbursed_derived - COALESCE(SUM(lt2.principal_portion_derived), 0)
    FROM m_loan ml
    LEFT JOIN m_loan_transaction lt2 ON lt2.loan_id = lt.loan_id
    WHERE ml.id = lt.loan_id
    AND lt2.transaction_date <= lt.transaction_date
    and lt2.transaction_type_enum = 2
    group by ml.principal_disbursed_derived
);

-- update loan summary
update
	m_loan ml
set
	principal_repaid_derived = (
	select
		coalesce(SUM(mlrs.principal_completed_derived),
		0)
	from
		m_loan_repayment_schedule mlrs
	where
		mlrs.principal_completed_derived is not null
		and mlrs.loan_id = ml.id
),
	interest_repaid_derived = (
	select
		coalesce(SUM(mlrs.interest_completed_derived),
		0)
	from
		m_loan_repayment_schedule mlrs
	where
		mlrs.interest_completed_derived is not null
		and mlrs.loan_id = ml.id
),
	fee_charges_repaid_derived = (
	select
		coalesce(SUM(mlrs.fee_charges_completed_derived),
		0)
	from
		m_loan_repayment_schedule mlrs
	where
		mlrs.fee_charges_completed_derived is not null
		and mlrs.loan_id = ml.id
),
	penalty_charges_repaid_derived = (
	select
		coalesce(SUM(mlrs.penalty_charges_completed_derived),
		0)
	from
		m_loan_repayment_schedule mlrs
	where
		mlrs.penalty_charges_completed_derived is not null
		and mlrs.loan_id = ml.id
);

update m_loan
set principal_outstanding_derived = principal_disbursed_derived - principal_repaid_derived,
	interest_outstanding_derived = interest_charged_derived - interest_repaid_derived,
	fee_charges_outstanding_derived = fee_charges_charged_derived - fee_charges_repaid_derived,
	penalty_charges_outstanding_derived = penalty_charges_charged_derived - penalty_charges_repaid_derived,
	total_repayment_derived = principal_repaid_derived + interest_repaid_derived + fee_charges_repaid_derived + penalty_charges_repaid_derived,
	total_outstanding_derived = principal_outstanding_derived + interest_outstanding_derived + fee_charges_outstanding_derived + penalty_charges_outstanding_derived;

-------------- Charge Payments -----------------------------
-- insert m_loan_charge_paid_by
insert into m_loan_charge_paid_by (loan_transaction_id, loan_charge_id, amount, installment_number)
select mlt.id, mlic.loan_charge_id, mlic.amount, mlrs.installment
from m_loan_installment_charge mlic join m_loan_repayment_schedule mlrs on mlic.loan_schedule_id = mlrs.id
join m_loan_transaction mlt on mlt.installment_id = mlrs.id
and (select count(1) from m_loan_charge_paid_by where loan_transaction_id = mlt.id and loan_charge_id = mlic.loan_charge_id) = 0

-- update m_loan_installment_charge with paid amount/status and outstanding
update m_loan_installment_charge
set amount_paid_derived = amount,
	amount_outstanding_derived = 0,
	is_paid_derived = true
where loan_schedule_id in (select id from m_loan_repayment_schedule mlrs where mlrs.completed_derived = true)

-- update m_loan_charge with paid and outstanding
update m_loan_charge mlc
set amount_paid_derived = (select sum(amount_paid_derived) from m_loan_installment_charge mlic where mlic.loan_charge_id = mlc.id and mlic.is_paid_derived = true);

update m_loan_charge
set amount_outstanding_derived = amount - amount_paid_derived
where amount_paid_derived is not null;

-- insert honorarios
INSERT INTO public.m_loan_charge
(loan_id, charge_id, is_penalty, charge_time_enum, due_for_collection_as_of_date, charge_calculation_enum, charge_payment_mode_enum, calculation_percentage, calculation_on_amount, charge_amount_or_percentage, amount, amount_outstanding_derived, is_paid_derived, is_active, submitted_on_date, applicable_from_installment, created_on_utc, last_modified_on_utc, created_by, last_modified_by)
select ml.id loan_id, mc.id charge_id, mc.is_penalty, mc.charge_time_enum, null::date due_for_collection_as_of_date, mc.charge_calculation_enum, mc.charge_payment_mode_enum, NULL::numeric calculation_percentage, NULL::numeric calculation_on_amount, 0 charge_amount_or_percentage, 0 amount, 0 amount_outstanding_derived, true is_paid_derived, true is_active, ml.disbursedon_date submitted_on_date, 1 applicable_from_installment, ml.disbursedon_date created_on_utc, ml.disbursedon_date last_modified_on_utc, 1 created_by, 1 last_modified_by from m_loan ml
join m_charge mc on mc.charge_calculation_enum = 1009
where ml.id not in (select mlc.loan_id from m_loan_charge mlc join m_charge mc on mlc.charge_id = mc.id where mc.charge_calculation_enum = 1009)
and ml.loan_status_id = 300

-- insert iva honorarios (assumption here is that there's only one Honorarios charge)
INSERT INTO public.m_loan_charge
(loan_id, charge_id, is_penalty, charge_time_enum, due_for_collection_as_of_date, charge_calculation_enum, charge_payment_mode_enum, calculation_percentage, calculation_on_amount, charge_amount_or_percentage, amount, amount_outstanding_derived, is_paid_derived, is_active, submitted_on_date, applicable_from_installment, created_on_utc, last_modified_on_utc, created_by, last_modified_by)
select ml.id loan_id, mc.id charge_id, mc.is_penalty, mc.charge_time_enum, null::date due_for_collection_as_of_date, mc.charge_calculation_enum, mc.charge_payment_mode_enum, mc.amount calculation_percentage, ml.principal_amount calculation_on_amount, mc.amount charge_amount_or_percentage, 0 amount, 0 amount_outstanding_derived, false is_paid_derived, true is_active, ml.disbursedon_date submitted_on_date, 1 applicable_from_installment, ml.disbursedon_date created_on_utc, ml.disbursedon_date last_modified_on_utc, 1 created_by, 1 last_modified_by from m_loan ml
join m_charge mc on mc.parent_charge_id = (select id from m_charge where charge_calculation_enum = 1009)
where ml.id not in (select mlc.loan_id from m_loan_charge mlc join m_charge mc on mlc.charge_id = mc.id where mc.parent_charge_id = (select id from m_charge where charge_calculation_enum = 1009))
and ml.loan_status_id = 300

-- insert m_loan_installment_charge for honorarios charges
INSERT INTO m_loan_installment_charge
(loan_charge_id, loan_schedule_id, due_date, amount)
select mlc.id loan_charge_id, mlrs.id loan_schedule_id, null::date due_date, 0 amount from m_loan ml join m_loan_charge mlc on ml.id = mlc.loan_id
join m_loan_repayment_schedule mlrs on ml.id = mlrs.loan_id
where mlc.charge_id in (4,5) -- (3,4) -- in UAT
and mlc.id not in (select loan_charge_id from m_loan_installment_charge where loan_charge_id = mlc.id and loan_schedule_id = mlrs.id)
order by mlc.id, mlrs.installment;


-- populate c_client_buy_process
insert into custom.c_client_buy_process (channel_id, client_id, point_if_sales_id, product_id, credit_id, requested_date, amount, term, created_at, created_by, ip_details, status, error_message, loan_id, interest_rate_points, codigo_seguro, cedula_seguro_voluntario)
SELECT (select id from custom.c_channel cc where cc.name = 'Tienda física'), ml.client_id, ccapos.id, ml.product_id, ml.id credit_id, ml.disbursedon_date requested_date, ml.principal_amount amount, ml.term_frequency term, CURRENT_DATE created_at, created_by, null::text ip_details, 200 status, null::text error_message, ml.id loan_id, 0 interest_rate_points, 0 codigo_seguro, 0 cedula_seguro_voluntario
from public.m_loan ml 
left join custom.c_client_ally_point_of_sales ccapos
on ml.migrar_code = ccapos.code 
where ml.loan_status_id = 300
and ml.is_migrated_loan = true
and ml.id not in (select loan_id from custom.c_client_buy_process)

-- UPDATE CLIENT STATUS
-- First run this query to check the client statuses in the migration data
select count(*), estadocli from tmp_clientes_migrar tcm group by tcm.estadocli;
-- Then run this query to check the existing client status
select * from m_blocking_reason_setting mbrs where level = 'CLIENT';
-- Add any missing statuses via the UI: Admin -> System -> Manage Blocking Reason Settings
-- Afterwards run these queries with the appropriate values for each status
update m_client mc1
set blocking_reason_id = case
 	--when tcm.estadocli = 'BLOQUEO POR INCONSISTENCIA EN INFORMACIÓN' then 24
    --when tcm.estadocli = 'MORA' then 6
	--when tc
	when tcm.estadocli = 'BLOQUEO POR INCONSISTENCIA EN INFORMACIÓN' then 11
	when tcm.estadocli = 'MORA' then 7
	when tcm.estadocli = 'BLOQUEO RIESGO DE CRÉDITO' then 9
	else null
end
from m_client mc2
inner join tmp_clientes_migrar tcm on mc2.external_id = cast(tcm.external_id as text)
where mc1.id = mc2.id

-- Run this to verify that the statuses are distributed right as per the migration data
select count(*), blocking_reason_id from m_client group by blocking_reason_id;

-- Update office id back to 1 for all clients
update m_client set office_id = 1;

-- Update interest_accrued_till date based on last payment date
update m_loan ml
set interest_accrued_till = txn.last_payment_date
from
(
	select MAX(transaction_date) last_payment_date, loan_id from m_loan_transaction mlt
	where mlt.transaction_type_enum = 2 and mlt.is_reversed = false
	group by loan_id
) txn
where ml.id = txn.loan_id


--- POST MIGRATION VALIDATIONS -----
-- Validate Aval Difference after Migration
select count(*) from (
select ml.id loan_id, ml.external_id, mlrs.installment, mlrs.fee_charges_amount, tcm.cpc_monto_aval, abs(tcm.cpc_monto_aval - mlrs.fee_charges_amount) diff from m_loan ml
join m_loan_repayment_schedule mlrs on ml.id = mlrs.loan_id
join tmp_creditos_migrar tcm on ml.external_id = tcm.external_id and tcm.cuo_nrocuota = mlrs.installment
where mlrs.fee_charges_amount != tcm.cpc_monto_aval
and abs(tcm.cpc_monto_aval - mlrs.fee_charges_amount) > 2
order by ml.external_id, mlrs.installment
) x;

--- Validate All Loans are Disbursed ---
select count(*) from m_loan where loan_status_id != 300;

--- Restore max legal rate data ---
delete from m_maximum_credit_rate_configuration;

insert into m_maximum_credit_rate_configuration select * from m_maximum_credit_rate_configuration_bk;

drop table m_maximum_credit_rate_configuration_bk;

-- Re-enable the following Jobs Before Migration --
-- Arrears Job
-- Penalties Job
-- Daily Accruals Job
-- Daily Charge Accruals Job

Run these scripts after the jobs run after migration
update m_ally_purchase_settlement set settlement_status = true;
update m_ally_collection_settlement set settlement_status = true;
