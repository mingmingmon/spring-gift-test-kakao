INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
INSERT INTO category (id, name) VALUES (1, '테스트카테고리');
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '테스트상품', 10000, 'http://image.png', 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본옵션', 10, 1);
