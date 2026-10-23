# Smart Banking

Hệ thống ngân hàng số (REST API) xây dựng bằng **Spring Boot 3**, **Java 21**, **MySQL 8** và **JWT**. Frontend tĩnh nằm trong thư mục `font_end` (khi triển khai).

## Cấu trúc thư mục

| Thư mục | Mô tả |
|---------|--------|
| `back_end/` | API Spring Boot |
| `font_end/` | Giao diện người dùng (HTML/JS/CSS) |
| `back_end/docker-compose.yml` | MySQL + API chạy bằng Docker |
| `back_end/docker-compose.host-db.yml` | API chạy với MySQL local đang chạy trên máy |

## Yêu cầu

- JDK 21
- Maven 3.9+ hoặc dùng `./mvnw`
- MySQL 8

## Chạy local (không Docker)

1. Tạo database MySQL: `smart_banking`
2. Tạo file cấu hình local:
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```
3. Sửa `spring.datasource.password` trong file vừa tạo.
4. Chạy API:
   ```bash
   ./mvnw spring-boot:run
   ```
   Trên Windows:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
5. API: `http://localhost:8081`
6. Swagger UI: `http://localhost:8081/swagger-ui/index.html`

## Chạy bằng Docker

Lưu ý quan trọng: bạn phải chạy command từ thư mục `back_end`.

### A) MySQL + API đều trong Docker

Tạo file `.env` từ `.env.example`:
```bash
cp .env.example .env
```

Chạy:
```bash
docker compose up -d --build
```

- MySQL host port: `3307`
- API port: `8081`
- Database mặc định: `smart_banking`
- Dữ liệu ban đầu sẽ rỗng nếu chưa import dump

Swagger: `http://localhost:8081/swagger-ui/index.html`

### B) API Docker + MySQL local (khuyến nghị khi đã có dữ liệu)

1. Đảm bảo MySQL local đang chạy và có database `smart_banking`
2. Tạo file `.env`:
   ```bash
   cp .env.example .env
   ```
3. Cập nhật biến:
   ```env
   HOST_MYSQL_HOST=host.docker.internal
   HOST_MYSQL_PORT=3306
   HOST_MYSQL_USER=root
   HOST_MYSQL_PASSWORD=your_local_mysql_password
   JWT_SECRET=your_secret_at_least_32_chars
   APP_FE_BASE_URL=http://localhost:5500
   ```
4. Chạy:
   ```bash
   docker compose -f docker-compose.host-db.yml up -d --build
   ```

### Import dữ liệu vào MySQL Docker

Xem hướng dẫn tại `docker/mysql-init/README.md`.

Dừng container nhưng giữ dữ liệu:
```bash
docker compose down
```

Reset DB trong Docker (xóa volume):
```bash
docker compose down -v
```

## Biến môi trường quan trọng

| Biến | Mô tả |
|------|--------|
| `SPRING_DATASOURCE_URL` | JDBC URL MySQL |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Tài khoản DB |
| `JWT_SECRET` | Chuỗi bí mật JWT (>= 32 ký tự) |
| `APP_FE_BASE_URL` | URL frontend (CORS) |

Profile `docker` dùng file `src/main/resources/application-docker.properties`.

## API chính

- `/api/v1/auth` — đăng ký, đăng nhập, refresh token
- `/api/v1/accounts` — tài khoản, tra cứu người nhận
- `/api/v1/transactions` — nạp, rút, chuyển tiền
- `/api/v1/payments` — thanh toán hóa đơn
- `/api/v1/admin` — quản trị (role ADMIN)

## Kiểm thử

```bash
./mvnw test
```

## GitHub Actions

CI đang chạy trên `.github/workflows/ci.yml`, kiểm tra `mvn test` với MySQL service.

## Bảo mật

- Không commit `.env`, `application-local.properties`, hoặc mật khẩu thật.
- Production: đổi `JWT_SECRET`, dùng HTTPS và mật khẩu mạnh.
