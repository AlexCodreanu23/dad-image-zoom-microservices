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

    // Drop and recreate table to ensure correct schema
    db.query(`DROP TABLE IF EXISTS pictures`, (err) => {
      if (err) console.error("Error dropping table:", err);
      
      db.query(`
        CREATE TABLE pictures (
          id INT AUTO_INCREMENT PRIMARY KEY,
          filename VARCHAR(255),
          data LONGBLOB
        )
      `, (err) => {
        if (err) console.error("Error creating table:", err);
        else console.log("Pictures table ready");
      });
    });

    startServer(db);
  });
}

function startServer(db) {
  // Upload picture
  app.post("/api/pictures", upload.single("file"), (req, res) => {
    const file = req.file;
    if (!file) {
      return res.status(400).json({ error: "No file uploaded" });
    }
    
    db.query(
      "INSERT INTO pictures (filename, data) VALUES (?, ?)",
      [file.originalname, file.buffer],
      (err, result) => {
        if (err) return res.status(500).send(err);
        console.log("C06: Picture saved with ID=" + result.insertId + ", filename=" + file.originalname);
        res.json({ id: result.insertId });
      }
    );
  });

  // Get picture by ID
  app.get("/api/pictures/:id", (req, res) => {
    const id = parseInt(req.params.id);
    if (isNaN(id)) {
      return res.status(400).json({ error: "Invalid ID" });
    }
    
    db.query(
      "SELECT * FROM pictures WHERE id = ?",
      [id],
      (err, rows) => {
        if (err) return res.status(500).send(err);
        if (rows.length === 0) return res.status(404).json({ error: "Picture not found" });
        
        res.set("Content-Type", "image/bmp");
        res.set("Content-Disposition", `inline; filename="${rows[0].filename}.bmp"`);
        res.send(rows[0].data);
      }
    );
  });

  // Get picture by filename (requestId)
  app.get("/api/pictures/name/:filename", (req, res) => {
    const filename = req.params.filename;
    
    db.query(
      "SELECT * FROM pictures WHERE filename = ? ORDER BY id DESC LIMIT 1",
      [filename],
      (err, rows) => {
        if (err) return res.status(500).send(err);
        if (rows.length === 0) {
          return res.status(404).json({ 
            error: "Picture not found", 
            message: "Image may still be processing. Please try again in a few seconds." 
          });
        }
        
        res.set("Content-Type", "image/bmp");
        res.set("Content-Disposition", `inline; filename="${filename}.bmp"`);
        res.send(rows[0].data);
      }
    );
  });

  // List all pictures (for debugging)
  app.get("/api/pictures", (req, res) => {
    db.query(
      "SELECT id, filename FROM pictures ORDER BY id DESC",
      (err, rows) => {
        if (err) return res.status(500).send(err);
        res.json(rows);
      }
    );
  });

  app.listen(7006, () => {
    console.log("C06 Node API running on port 7006");
  });
}

connectWithRetry();