CREATE TABLE operator
(
    id          SERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL
);

INSERT INTO operator(name)
VALUES ('PSP'),
       ('G4S'),
       ('Serco'),
       ('Sodexo'),
       ('MTCNovo');

CREATE TABLE prison_operator
(
    prison_id VARCHAR(6) NOT NULL REFERENCES prison,
    operator_id INT NOT NULL REFERENCES operator,
    PRIMARY KEY (prison_id, operator_id)
);

INSERT INTO prison_operator(prison_id, operator_id)
VALUES
('ACI', 2),
('AGI', 1),
('AKI', 1),
('ALI', 1),
('ANI', 1),
('ASI', 3),
('AWI', 1),
('AYI', 1),
('BAI', 1),
('BCI', 1),
('BDI', 1),
('BFI', 1),
('BHI', 1),
('BKI', 1),
('BLI', 1),
('BMI', 1),
('BNI', 1),
('BRI', 1),
('BSI', 1),
('BTI', 1),
('BUI', 1),
('BWI', 1),
('BXI', 1),
('BZI', 4),
('CDI', 1),
('CEI', 1),
('CFI', 1),
('CHI', 1),
('CKI', 1),
('CLI', 1),
('CPI', 1),
('CRI', 1),
('CSI', 1),
('CWI', 1),
('CYI', 1),
('DAI', 1),
('DGI', 3),
('DHI', 1),
('DMI', 1),
('DNI', 3),
('DRI', 1),
('DTI', 1),
('DVI', 1),
('DWI', 1),
('EEI', 1),
('EHI', 1),
('ESI', 1),
('EVI', 1),
('EWI', 1),
('EXI', 1),
('EYI', 1),
('FBI', 4),
('FDI', 1),
('FEI', 1),
('FHI', 1),
('FII', 1),
('FKI', 1),
('FMI', 1),
('FNI', 1),
('FOI', 1),
('FSI', 1),
('FWI', 1),
('FYI', 1),
('GHI', 1),
('GLI', 1),
('GMI', 1),
('GNI', 1),
('GPI', 1),
('GTI', 1),
('HBI', 1),
('HCI', 1),
('HDI', 1),
('HEI', 1),
('HGI', 1),
('HHI', 1),
('HII', 1),
('HLI', 1),
('HMI', 1),
('HOI', 1),
('HPI', 1),
('HRI', 1),
('HVI', 1),
('HYI', 1),
('ISI', 1),
('IWI', 1),
('KMI', 1),
('KTI', 1),
('KVI', 1),
('LAI', 1),
('LCI', 1),
('LEI', 1),
('LFI', 1),
('LGI', 3),
('LHI', 1),
('LII', 1),
('LLI', 1),
('LMI', 1),
('LNI', 1),
('LPI', 1),
('LTI', 1),
('LWI', 1),
('LYI', 1),
('MDI', 1),
('MHI', 1),
('MRI', 1),
('MSI', 1),
('MTI', 1),
('MWI', 1),
('NEI', 1),
('NHI', 1),
('NLI', 4),
('NMI', 1),
('NNI', 1),
('NOI', 1),
('NSI', 1),
('NWI', 1),
('ONI', 1),
('OWI', 2),
('OXI', 1),
('PBI', 4),
('PCI', 1),
('PDI', 1),
('PFI', 4),
('PKI', 1),
('PNI', 1),
('PRI', 2),
('PTI', 1),
('PVI', 1),
('PYI', 2),
('RCI', 1),
('RDI', 1),
('RHI', 2),
('RLI', 1),
('RNI', 1),
('RSI', 1),
('RUI', 1),
('SDI', 1),
('SFI', 1),
('SHI', 1),
('SKI', 1),
('SLI', 1),
('SMI', 1),
('SNI', 1),
('SPI', 1),
('STC2', 5),
('STC3', 2),
('STI', 1),
('SUI', 1),
('SWI', 1),
('SYI', 1),
('TAI', 1),
('TCI', 1),
('TSI', 3),
('UKI', 1),
('UPI', 1),
('VEI', 1),
('WAI', 1),
('WBI', 1),
('WCI', 1),
('WDI', 1),
('WEI', 1),
('WHI', 1),
('WII', 1),
('WLI', 1),
('WMI', 1),
('WNI', 1),
('WOI', 1),
('WRI', 1),
('WSI', 1),
('WTI', 1),
('WWI', 1),
('WYI', 1);