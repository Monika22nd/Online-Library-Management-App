-- SQL script for setting up the 'quanlythuvien2' database.
-- This script creates the necessary tables and populates them with sample data.

-- Create the database if it does not already exist.
-- It uses utf8mb4 for character set to support a wide range of characters.
CREATE DATABASE IF NOT EXISTS `quanlythuvien2` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Switch to the newly created or existing database.
USE `quanlythuvien2`;

--
-- Table structure for table `authors`
-- This table stores information about the book authors.
--
DROP TABLE IF EXISTS `authors`;
CREATE TABLE `authors` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `biography` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `authors`
--
INSERT INTO `authors` VALUES 
(1,'J.K. Rowling','British author, best known for the Harry Potter series.'),
(2,'J.R.R. Tolkien','English writer, poet, philologist, and academic.'),
(3,'George Orwell','English novelist, essayist, journalist and critic.');

--
-- Table structure for table `users`
-- This table stores user information and credentials.
--
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('ADMIN','CLIENT') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CLIENT',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
-- In a real-world application, passwords should always be hashed.
--
INSERT INTO `users` VALUES 
(1,'Admin User','admin','adminpass','admin@library.com','111222333','ADMIN'),
(2,'Test Client','testuser','userpass','client@library.com','444555666','CLIENT');

--
-- Table structure for table `books`
-- This table stores details about the books in the library.
--
DROP TABLE IF EXISTS `books`;
CREATE TABLE `books` (
  `id` int NOT NULL AUTO_INCREMENT,
  `isbn` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `author_id` int DEFAULT NULL,
  `genre` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` double DEFAULT '0',
  `copies_available` int DEFAULT '0',
  `description` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `author_id` (`author_id`),
  CONSTRAINT `books_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `authors` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `books`
--
INSERT INTO `books` VALUES 
(1,'978-0747532699','Harry Potter and the Sorcerer\'s Stone',1,'Fantasy',19.99,10,'The first book in the Harry Potter series.'),
(2,'978-0618640157','The Lord of the Rings',2,'Fantasy',29.99,5,'A high-fantasy novel by J. R. R. Tolkien.'),
(3,'978-0451524935','1984',3,'Dystopian',15,7,'A dystopian social science fiction novel and cautionary tale.'),
(4,'978-0747538493','Harry Potter and the Chamber of Secrets',1,'Fantasy',19.99,8,'The second book in the Harry Potter series.');

--
-- Table structure for table `loans`
-- This table tracks book borrowing records.
--
DROP TABLE IF EXISTS `loans`;
CREATE TABLE `loans` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `book_id` int NOT NULL,
  `borrow_date` date NOT NULL,
  `due_date` date NOT NULL,
  `return_date` date DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'BORROWED', -- e.g., BORROWED, RETURNED
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `book_id` (`book_id`),
  CONSTRAINT `loans_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `loans_ibfk_2` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `loans`
--
INSERT INTO `loans` VALUES 
(1,2,1,'2025-11-01','2025-11-15',NULL,'BORROWED');
