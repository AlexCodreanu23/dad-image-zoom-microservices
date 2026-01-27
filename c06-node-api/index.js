const express = require("express");
const mysql = require("mysql2");
const multer = require("multer");

const app = express();
const upload = multer();

function connectWithRetry() {
  console.log("Trying to connect to MySQL...");

  const db = mysql.createConnection({
    host: "mysql",
    user: "root",
    password: "root",
    database: "images_db"
  });

  db.connect(err => {
    if (err) {
      console.error("MySQL not ready, retrying in 5 seconds...");
      setTimeout(connectWithRetry, 5000);
      return;
    }

    console.log("Connected to MySQL");

    db.query(`
      CREATE TABLE IF NOT EXISTS pictures (
        id INT AUTO_INCREMENT PRIMARY KEY,
        filename VARCHAR(255),
        data LONGBLOB
      )
    `);

    startServer(db);
  });
}

function startServer(db) {
  app.post("/api/pictures", upload.single("file"), (req, res) => {
    const file = req.file;

    db.query(
      "INSERT INTO pictures (filename, data) VALUES (?, ?)",
      [file.originalname, file.buffer],
      (err, result) => {
        if (err) return res.status(500).send(err);
        res.json({ id: result.insertId });
      }
    );
  });

  app.get("/api/pictures/:id", (req, res) => {
    db.query(
      "SELECT * FROM pictures WHERE id = ?",
      [req.params.id],
      (err, rows) => {
        if (err || rows.length === 0) return res.sendStatus(404);
        res.set("Content-Type", "image/bmp");
        res.send(rows[0].data);
      }
    );
  });

  app.listen(7006, () => {
    console.log("C06 Node API running on port 7006");
  });
}

connectWithRetry();
