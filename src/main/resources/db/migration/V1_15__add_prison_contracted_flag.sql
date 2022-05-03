ALTER TABLE prison ADD COLUMN contracted BOOLEAN NOT NULL DEFAULT false;

UPDATE prison SET contracted = true WHERE prison_id IN ('ACI', 'ASI', 'BZI', 'DNI', 'DGI', 'FWI', 'FBI', 'LGI', 'NLI', 'OWI', 'PBI', 'PFI', 'PRI', 'RHI', 'TSI');