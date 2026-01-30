const express = require("express");
const mysql = require("mysql2");
const { MongoClient } = require("mongodb");
const multer = require("multer");
const os = require("os");

const app = express();

// Increase payload limits
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));

// Configure multer with larger file size limit (50MB)
const upload = multer({
  limits: { fileSize: 50 * 1024 * 1024 }
});

// MongoDB connection
let mongoDb;
const mongoUrl = "mongodb://mongodb:27017";
const mongoDbName = "snmp_db";

async function connectMongo() {
  try {
    const client = new MongoClient(mongoUrl);
    await client.connect();
    mongoDb = client.db(mongoDbName);
    console.log("Connected to MongoDB");
    
    // Insert initial SNMP data for all containers
    await initSnmpData();
  } catch (err) {
    console.error("MongoDB connection failed, retrying in 5 seconds...", err.message);
    setTimeout(connectMongo, 5000);
  }
}

async function initSnmpData() {
  const collection = mongoDb.collection("snmp");
  
  // Clear old data
  await collection.deleteMany({});
  
  // Simulated SNMP data for all containers
  const containers = [
    { name: "c01-backend", type: "Javalin REST API", port: 7001 },
    { name: "c02-jms-broker", type: "ActiveMQ JMS Broker", port: 61616 },
    { name: "c03-ejb-mdb", type: "TomEE MDB + RMI Client", port: 8080 },
    { name: "c04-rmi-server-1", type: "RMI Server", port: 1104 },
    { name: "c05-rmi-server-2", type: "RMI Server", port: 1105 },
    { name: "c06-node-api", type: "Node.js REST API", port: 7006 },
    { name: "mysql", type: "MySQL Database", port: 3306 },
    { name: "mongodb", type: "MongoDB Database", port: 27017 }
  ];
  
  const snmpData = containers.map(container => ({
    container: container.name,
    type: container.type,
    port: container.port,
    os: {
      name: "Linux",
      platform: "linux",
      release: "5.15.0",
      arch: "x64"
    },
    cpu: {
      usage: (Math.random() * 30 + 5).toFixed(2) + "%",
      cores: 4
    },
    memory: {
      total: "8192 MB",
      used: (Math.random() * 2000 + 500).toFixed(0) + " MB",
      usage: (Math.random() * 40 + 10).toFixed(2) + "%"
    },
    timestamp: new Date()
  }));
  
  await collection.insertMany(snmpData);
  console.log("SNMP data initialized for " + containers.length + " containers");
}

// MySQL connection
function connectMySQL() {
  console.log("Trying to connect to MySQL...");
  const db = mysql.createConnection({
    host: "mysql",
    user: "root",
    password: "root",
    database: "images_db",
    maxAllowedPacket: 64 * 1024 * 1024
  });

  db.connect(err => {
    if (err) {
      console.error("MySQL not ready, retrying in 5 seconds...");
      setTimeout(connectMySQL, 5000);
      return;
    }
    console.log("Connected to MySQL");

    db.query("SET GLOBAL max_allowed_packet=67108864", (err) => {
      if (err) console.log("Note: Could not set max_allowed_packet");
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
  // ==================== PICTURES API (MySQL) ====================
  
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

  // List all pictures
  app.get("/api/pictures", (req, res) => {
    db.query(
      "SELECT id, filename FROM pictures ORDER BY id DESC",
      (err, rows) => {
        if (err) return res.status(500).send(err);
        res.json(rows);
      }
    );
  });

  // ==================== SNMP API (MongoDB) ====================
  
  // Get all SNMP data
  app.get("/api/snmp", async (req, res) => {
    try {
      if (!mongoDb) {
        return res.status(503).json({ error: "MongoDB not connected" });
      }
      
      const collection = mongoDb.collection("snmp");
      const data = await collection.find({}).toArray();
      res.json(data);
    } catch (err) {
      res.status(500).json({ error: err.message });
    }
  });

  // Get SNMP data for specific container
  app.get("/api/snmp/:container", async (req, res) => {
    try {
      if (!mongoDb) {
        return res.status(503).json({ error: "MongoDB not connected" });
      }
      
      const collection = mongoDb.collection("snmp");
      const data = await collection.findOne({ container: req.params.container });
      
      if (!data) {
        return res.status(404).json({ error: "Container not found" });
      }
      
      res.json(data);
    } catch (err) {
      res.status(500).json({ error: err.message });
    }
  });

  // Refresh SNMP data (simulate new readings)
  app.post("/api/snmp/refresh", async (req, res) => {
    try {
      if (!mongoDb) {
        return res.status(503).json({ error: "MongoDB not connected" });
      }
      
      await initSnmpData();
      res.json({ message: "SNMP data refreshed" });
    } catch (err) {
      res.status(500).json({ error: err.message });
    }
  });

  // ==================== START SERVER ====================
  
  app.listen(7006, () => {
    console.log("C06 Node API running on port 7006");
    console.log("  - Pictures API: /api/pictures");
    console.log("  - SNMP API: /api/snmp");
  });
}

// Start connections
connectMongo();
connectMySQL();