/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@SpringBootApplication
public class Main {

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    System.out.println("Print 'SpringApplication.run' by System.out");
    logger.info("Print 'SpringApplication.run' by logger");
    SpringApplication.run(Main.class, args);
  }

  @RequestMapping("/")
  String index() {
    return "index";
  }

  @RequestMapping("/db")
  String db(Map<String, Object> model) {
    System.out.println("dbUrl:" + dbUrl);
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        output.add("Read from DB: " + rs.getTimestamp("tick"));
      }

      model.put("records", output);
      model.put("dbUrl", dbUrl);
      return "db";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @RequestMapping("/sha256")
  String sha256(Map<String, Object> model) {
    logger.info("sha256 was called");
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT sfid, email FROM helloherokupostgresql.contact");
      StringBuilder stb = new StringBuilder();

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        //output.add("Read from DB: " + rs.getTimestamp("tick"));
        String sfid = rs.getString("sfid");
        String email = rs.getString("email");
        byte[] cipher_byte;
        StringBuilder sbsha256;
        
        if (email == null || email.equals("")) {
          sbsha256 = new StringBuilder("");
        } else {
          MessageDigest md = MessageDigest.getInstance("SHA-256");
          md.update(email.getBytes());
          cipher_byte = md.digest();
          sbsha256 = new StringBuilder(2 * cipher_byte.length);
          for(byte b: cipher_byte) {
             sbsha256.append(String.format("%02x", b&0xff) );
          }
        }

        output.add("email:" + email + ", sha256:" + sbsha256.toString());
        logger.info("email:" + email + ", sha256:" + sbsha256.toString());

        stb.append("update helloherokupostgresql.contact set Email_SHA256__c = '" + sbsha256.toString() + "' where sfid = '" + sfid + "';");
      }

      //stmt.executeUpdate(stb.toString());

      model.put("records", output);
      // model.put("dbUrl", dbUrl);

      return "sha256";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      logger.info(e.getMessage());
      return "error";
    }
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    System.out.println("dbUrl:" + dbUrl);
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

}
