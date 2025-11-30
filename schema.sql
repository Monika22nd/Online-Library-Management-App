CREATE DATABASE  IF NOT EXISTS `quanlythuvien2` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `quanlythuvien2`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: quanlythuvien2
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `authors`
--

DROP TABLE IF EXISTS `authors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `authors` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `biography` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `authors`
--

LOCK TABLES `authors` WRITE;
/*!40000 ALTER TABLE `authors` DISABLE KEYS */;
INSERT INTO `authors` VALUES (1,'J.K. Rowling','Tác giả người Anh, nổi tiếng với bộ truyện Harry Potter.'),(2,'Nguyễn Nhật Ánh','Nhà văn Việt Nam chuyên viết cho thanh thiếu niên.'),(3,'Paulo Coelho','Tiểu thuyết gia người Brazil, tác giả của Nhà Giả Kim.'),(4,'Robert C. Martin','Kỹ sư phần mềm nổi tiếng, tác giả của Clean Code.'),(5,'Agatha Christie','Nữ hoàng trinh thám người Anh.'),(6,'Fujiko F. Fujio','Họa sĩ truyện tranh Nhật Bản, cha đẻ của Doraemon.'),(7,'George Orwell','Nhà văn nổi tiếng với các tác phẩm châm biếm chính trị.'),(8,'Dale Carnegie','Tác giả sách Đắc Nhân Tâm.'),(9,'J.R.R. Tolkien','Cha đẻ của dòng văn học giả tưởng (High Fantasy), tác giả Chúa Nhẫn.'),(10,'Frank Herbert','Tác giả vĩ đại của dòng khoa học viễn tưởng với tác phẩm Dune.'),(11,'Arthur Conan Doyle','Cha đẻ của thám tử Sherlock Holmes.'),(12,'George R.R. Martin','Tác giả của bộ sử thi Trò chơi vương quyền.'),(13,'Nam Cao','Nhà văn hiện thực xuất sắc của Việt Nam.'),(14,'Nicholas Sparks','Ông vua của dòng tiểu thuyết lãng mạn hiện đại.'),(15,'Antoine de Saint-Exupéry','Nhà văn, phi công Pháp, tác giả Hoàng Tử Bé.'),(16,'Stephen R. Covey','Chuyên gia về nghệ thuật sống và lãnh đạo.'),(17,'Tô Hoài','Nhà văn nổi tiếng với các tác phẩm viết cho thiếu nhi Việt Nam.'),(18,'Jostein Gaarder','Nhà văn Na Uy nổi tiếng với các tác phẩm triết học nhập môn.'),(19,'Albert Camus','Nhà văn, triết gia Pháp, giải Nobel Văn học.');
/*!40000 ALTER TABLE `authors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `id` int NOT NULL AUTO_INCREMENT,
  `isbn` varchar(20) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `genre` varchar(50) DEFAULT NULL,
  `price` double DEFAULT NULL,
  `copies_available` int DEFAULT '0',
  `description` text,
  `author_id` int DEFAULT NULL,
  `image_path` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `isbn` (`isbn`),
  KEY `fk_book_author` (`author_id`),
  CONSTRAINT `fk_book_author` FOREIGN KEY (`author_id`) REFERENCES `authors` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (1,'978-01','Harry Potter và Hòn đá Phù thủy','Giả tưởng',150000,5,'Cậu bé Harry Potter phát hiện ra mình là phù thủy và nhập học Hogwarts.',1,NULL),(2,'978-02','Harry Potter và Phòng chứa Bí mật','Giả tưởng',160000,3,'Năm học thứ hai đầy biến cố của Harry tại Hogwarts.',1,NULL),(3,'978-03','Mắt Biếc','Lãng mạn',110000,10,'Câu chuyện tình đơn phương buồn bã của Ngạn dành cho Hà Lan.',2,NULL),(4,'978-04','Kính Vạn Hoa - Tập 1','Thiếu nhi',85000,8,'Những câu chuyện học trò tinh nghịch của Quý ròm, Tiểu Long và Hạnh.',2,NULL),(5,'978-05','Tôi thấy hoa vàng trên cỏ xanh','Truyện dài',120000,0,'Vé đi tuổi thơ qua câu chuyện của anh em Thiều và Tường (Đang hết hàng).',2,NULL),(6,'978-06','Nhà Giả Kim','Triết lý',79000,15,'Hành trình đi tìm kho báu và lắng nghe tiếng gọi trái tim của chàng chăn cừu Santiago.',3,NULL),(7,'978-07','Clean Code (Mã Sạch)','Công nghệ',350000,5,'Cuốn sách gối đầu giường cho mọi lập trình viên muốn viết code tốt hơn.',4,NULL),(8,'978-08','The Clean Coder','Công nghệ',320000,2,'Quy tắc ứng xử và thái độ làm việc chuyên nghiệp của lập trình viên.',4,NULL),(9,'978-09','Án mạng trên chuyến tàu tốc hành Phương Đông','Trinh thám',105000,4,'Vụ án hóc búa nhất của thám tử Hercule Poirot trên tàu hỏa.',5,NULL),(10,'978-10','Mười người da đen nhỏ','Trinh thám',98000,6,'Vụ án bí ẩn trên hòn đảo không lối thoát.',5,NULL),(11,'978-11','Doraemon - Tập 1','Truyện tranh',25000,20,'Chú mèo máy đến từ tương lai.',6,NULL),(12,'978-12','Doraemon Truyện dài: Nobita và hành tinh muông thú','Truyện tranh',35000,12,'Cuộc phiêu lưu đến hành tinh lạ.',6,NULL),(13,'978-13','1984','Khoa học viễn tưởng',130000,5,'Một xã hội giả tưởng bị kiểm soát toàn diện.',7,NULL),(14,'978-14','Chuyện ở nông trại (Animal Farm)','Ngụ ngôn',90000,7,'Câu chuyện ngụ ngôn về các con vật nổi dậy ở nông trại.',7,NULL),(15,'978-15','Đắc Nhân Tâm','Kỹ năng sống',86000,50,'Nghệ thuật thu phục lòng người.',8,NULL),(16,'978-16','Harry Potter và Tên tù nhân ngục Azkaban','Giả tưởng',170000,8,'Harry đối mặt với Sirius Black.',1,NULL),(17,'978-17','Harry Potter và Chiếc cốc lửa','Giả tưởng',220000,6,'Cuộc thi Tam Pháp Thuật đầy nguy hiểm.',1,NULL),(18,'978-18','Harry Potter và Hội Phượng Hoàng','Giả tưởng',250000,10,'Harry cùng hội kín chống lại Voldemort.',1,NULL),(19,'978-19','Harry Potter và Hoàng tử lai','Giả tưởng',210000,7,'Khám phá quá khứ của Voldemort.',1,NULL),(20,'978-20','Harry Potter và Bảo bối Tử thần','Giả tưởng',280000,12,'Trận chiến cuối cùng tại Hogwarts.',1,NULL),(21,'978-21','Anh Chàng Hobbit','Giả tưởng',135000,15,'Cuộc phiêu lưu của Bilbo Baggins.',9,NULL),(22,'978-22','Chúa Nhẫn 1: Đoàn Hộ Nhẫn','Giả tưởng',185000,10,'Frodo bắt đầu hành trình hủy nhẫn.',9,NULL),(23,'978-23','Chúa Nhẫn 2: Hai Tòa Tháp','Giả tưởng',185000,8,'Cuộc chiến tại Helm Deep.',9,NULL),(24,'978-24','Chúa Nhẫn 3: Nhà Vua Trở Về','Giả tưởng',195000,9,'Trận chiến cuối cùng tại Gondor.',9,NULL),(25,'978-25','Dune (Xứ Cát)','Khoa học viễn tưởng',210000,20,'Cuộc chiến tranh giành hương dược trên hành tinh cát.',10,NULL),(26,'978-26','Dune Messiah (Cứu tinh xứ Cát)','Khoa học viễn tưởng',160000,5,'Paul Atreides đối mặt với âm mưu lật đổ.',10,NULL),(27,'978-27','Children of Dune','Khoa học viễn tưởng',175000,6,'Số phận những đứa con của Paul.',10,NULL),(28,'978-29','Sherlock Holmes: Cuộc điều tra màu đỏ','Trinh thám',85000,12,'Vụ án đầu tiên của Holmes và Watson.',11,NULL),(29,'978-30','Sherlock Holmes: Dấu bộ tứ','Trinh thám',95000,8,'Truy tìm kho báu bị đánh cắp.',11,NULL),(30,'978-31','A Game of Thrones (Trò chơi vương quyền)','Giả tưởng',250000,5,'Mùa đông đang đến tại Westeros.',12,NULL),(31,'978-32','A Clash of Kings','Giả tưởng',260000,4,'Cuộc chiến của 5 vị vua.',12,NULL),(32,'978-33','Chí Phèo','Văn học Việt Nam',75000,25,'Bi kịch của người nông dân bị tha hóa.',13,NULL),(33,'978-34','Sống Mòn','Văn học Việt Nam',82000,10,'Nỗi đau của người trí thức nghèo.',13,NULL),(34,'978-35','Quẳng gánh lo đi và vui sống','Kỹ năng sống',96000,30,'Cách để giảm thiểu lo lắng và tận hưởng cuộc sống.',8,NULL),(35,'978-36','7 Thói quen để thành đạt','Kỹ năng sống',185000,20,'Cuốn sách kinh điển về phát triển bản thân và lãnh đạo.',16,NULL),(36,'978-37','Hoàng Tử Bé','Ngụ ngôn',75000,40,'Câu chuyện ngụ ngôn triết lý sâu sắc dành cho người lớn.',15,NULL),(37,'978-38','Dế Mèn phiêu lưu ký','Ngụ ngôn',65000,25,'Cuộc phiêu lưu của Dế Mèn và bài học về đường đời.',17,NULL),(38,'978-39','Cô gái đến từ hôm qua','Lãng mạn',110000,15,'Câu chuyện tình yêu tuổi học trò đầy hoài niệm.',2,NULL),(39,'978-40','Nhật ký lưu bút (The Notebook)','Lãng mạn',135000,10,'Một câu chuyện tình yêu vĩnh cửu đầy xúc động.',14,NULL),(40,'978-41','Thế giới của Sophie','Triết lý',198000,8,'Cuốn tiểu thuyết dẫn nhập vào lịch sử triết học phương Tây.',18,NULL),(41,'978-42','Người xa lạ (L\'Étranger)','Triết lý',90000,12,'Tác phẩm tiêu biểu của chủ nghĩa hiện sinh.',19,NULL);
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loans`
--

DROP TABLE IF EXISTS `loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loans` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `book_id` int NOT NULL,
  `borrow_date` date NOT NULL,
  `due_date` date NOT NULL,
  `return_date` date DEFAULT NULL,
  `status` enum('PENDING','BORROWED','RETURNED','REJECTED') DEFAULT 'PENDING',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `book_id` (`book_id`),
  CONSTRAINT `loans_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `loans_ibfk_2` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loans`
--

LOCK TABLES `loans` WRITE;
/*!40000 ALTER TABLE `loans` DISABLE KEYS */;
INSERT INTO `loans` VALUES (1,3,4,'2025-11-29','2025-12-13','2025-11-29','RETURNED'),(2,3,3,'2025-11-29','2025-12-13','2025-11-29','RETURNED'),(3,3,2,'2025-11-29','2025-12-13','2025-11-29','RETURNED'),(4,3,13,'2025-11-29','2025-12-13','2025-11-29','RETURNED'),(5,3,14,'2025-11-29','2025-12-13','2025-11-29','RETURNED'),(6,3,3,'2025-11-30','2025-12-14','2025-11-30','RETURNED'),(7,3,4,'2025-11-30','2025-12-14','2025-11-30','RETURNED'),(8,3,6,'2025-11-30','2025-12-14','2025-11-30','RETURNED'),(9,3,11,'2025-11-30','2025-12-14','2025-11-30','RETURNED'),(10,3,12,'2025-11-30','2025-12-14','2025-11-30','RETURNED'),(11,3,10,'2025-11-30','2025-12-14','2025-11-30','RETURNED');
/*!40000 ALTER TABLE `loans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `role` enum('ADMIN','CLIENT') NOT NULL DEFAULT 'CLIENT',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Administrator','admin','1','admin@library.com','0900000001','ADMIN'),(2,'Nguyen Van A','testuser','1','userA@gmail.com','0900000002','CLIENT'),(3,'Tran Thi B','client2','123','userB@gmail.com','0900000003','CLIENT');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-30 22:49:51
