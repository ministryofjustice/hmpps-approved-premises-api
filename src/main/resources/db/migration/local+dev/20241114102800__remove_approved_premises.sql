-- clear down all approved premises as this is now being managed by seed jobs in seed.local+dev+test
DELETE FROM approved_premises;
DELETE FROM premises WHERE service = 'approved-premises';