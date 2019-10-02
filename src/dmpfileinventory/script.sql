CREATE TABLE `file_register` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`name` varchar(100) NOT NULL,
`last_modified_at` datetime NOT NULL,
`size` bigint(20) NOT NULL,
`total_line` int NULL,
`status` int NOT NULL,
`created_at` datetime NOT NULL,
`updated_at` datetime NULL,
 PRIMARY KEY (`id`)
) 