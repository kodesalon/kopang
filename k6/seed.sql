-- k6 fairness-test 전용 시드 데이터
-- H2 콘솔(http://localhost:8080/h2-console)에서 실행
-- JDBC URL: jdbc:h2:mem:kopang  /  User: sa  /  Password: (빈칸)
--
-- 주의: 앱이 실행 중일 때만 유효 (H2 in-memory는 앱 재시작 시 초기화)

SET FOREIGN_KEY_CHECKS = 0;

-- 기존 데이터 초기화 (재실행 시 중복 방지)
DELETE FROM order_product;
DELETE FROM orders;
DELETE FROM payments;
DELETE FROM order_stock_event;
DELETE FROM stock;
DELETE FROM member_address;
DELETE FROM warehouse;
DELETE FROM product;

-- 1. 상품
INSERT INTO product (no, name, description, price)
VALUES (1, '선착순 이벤트 상품', '공정성 테스트용', 10000);

-- 2. 창고 (region은 WarehouseRegion enum 값 사용)
INSERT INTO warehouse (no, name, region, zip_code, address, detail, latitude, longitude)
VALUES (1, '서울 창고', 'SEOUL', '05717', '서울시 송파구 올림픽로', NULL, 37.51, 127.01);

-- 3. 재고 (DB 레코드 — Redis와 별개)
INSERT INTO stock (no, product_no, warehouse_no, quantity)
VALUES (1, 1, 1, 100);

-- 4. 회원 주소 (member_no=1 단일 회원으로 테스트)
INSERT INTO member_address (no, member_no, alias, zip_code, address, detail, latitude, longitude, is_default)
VALUES (1, 1, '집', '06164', '서울시 강남구 테헤란로', NULL, 37.5, 127.0, true);

SET FOREIGN_KEY_CHECKS = 1;

-- 확인
SELECT 'product'       AS tbl, COUNT(*) AS cnt FROM product       UNION ALL
SELECT 'warehouse'     AS tbl, COUNT(*) AS cnt FROM warehouse      UNION ALL
SELECT 'stock'         AS tbl, COUNT(*) AS cnt FROM stock          UNION ALL
SELECT 'member_address'AS tbl, COUNT(*) AS cnt FROM member_address;