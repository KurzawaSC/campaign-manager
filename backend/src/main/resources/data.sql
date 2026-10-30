INSERT INTO seller (name) VALUES ('Demo Seller');

INSERT INTO emerald_account (balance, version, seller_id)
SELECT 10000.00, 0, id FROM seller WHERE name = 'Demo Seller';

INSERT INTO product (name, seller_id)
SELECT product_name, seller.id
FROM (VALUES ('Wireless Headphones'), ('Smart Watch'), ('Running Shoes'), ('Coffee Grinder'), ('Desk Lamp')) AS products(product_name)
CROSS JOIN seller
WHERE seller.name = 'Demo Seller';

INSERT INTO town (name) VALUES
('Bialystok'), ('Gdansk'), ('Katowice'), ('Krakow'), ('Lodz'),
('Lublin'), ('Poznan'), ('Szczecin'), ('Warsaw'), ('Wroclaw');

INSERT INTO keyword (name) VALUES
('accessories'), ('audio'), ('bestseller'), ('bluetooth'), ('coffee'),
('deal'), ('discount'), ('electronics'), ('fashion'), ('fitness'),
('gadgets'), ('gift'), ('headphones'), ('home'), ('kitchen'),
('lighting'), ('new arrival'), ('office'), ('outdoor'), ('premium'),
('running'), ('sale'), ('shoes'), ('smart home'), ('sport'),
('summer'), ('wearables'), ('wireless'), ('winter');
