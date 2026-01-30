const express = require("express");
const mysql = require("mysql2");
const multer = require("multer");
const app = express();

// Increase payload limits
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));

// Configure multer with larger file size limit (50MB)
const upload = multer({
  limits: { fileSize: 50 * 1024 * 1024 }
});

function connectWithRetry() {
  console.log("Trying to connect to MySQL...");
  const db = mysql.createConnection({
    host: "mysql",
    user: "root",
    password: "root",
    database: "images_db",
    maxAllowedPacket: 64 * 1024 * 1024 // 64MB for large BLOBs
  });

  db.connect(err => {
    if (err) {
      console.error("MySQL not ready, retrying in 5 seconds...");
      setTimeout(connectWithRetry, 5000);
      return;
    }
    console.log("Connected to MySQL");

    // Increase MySQL packet size for large images
    db.query("SET GLOBAL max_allowed_packet=67108864", (err) => {
      if (err) console.log("Note: Could not set max_allowed_packet (need SUPER privilege)");
    });

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
  // Upload picture
  app.post("/api/pictures", upload.single("file"), (req, res) => {
    const file = req.file;
    if (!file) {
      return res.status(400).json({ error: "No file uploaded" });
    }
    
    console.log("C06: Receiving file, size=" + file.buffer.length + " bytes");
    
    db.query(
      "INSERT INTO pictures (filename, data) VALUES (?, ?)",
      [file.originalname, file.buffer],
      (err, result) => {
        if (err) {
          console.error("C06: MySQL error:", err.message);
          return res.status(500).json({ error: err.message });
        }
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