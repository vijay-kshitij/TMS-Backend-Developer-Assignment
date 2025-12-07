-- ============================================
-- TMS Database Constraints and Indexes
-- ============================================
-- Run this script after Spring Boot creates the tables
-- Or let Hibernate create tables first, then run this

-- 1. Create unique partial index: Only one ACCEPTED bid per load
-- This ensures the business rule that a load can only have one accepted bid
CREATE UNIQUE INDEX IF NOT EXISTS uk_one_accepted_bid_per_load
ON bid (load_id)
WHERE status = 'ACCEPTED';

-- 2. Add foreign key constraints (if not created by Hibernate)
-- These ensure referential integrity between tables

-- Bid table foreign keys
ALTER TABLE bid
ADD CONSTRAINT IF NOT EXISTS fk_bid_load
FOREIGN KEY (load_id) REFERENCES load(load_id) ON DELETE CASCADE;

ALTER TABLE bid
ADD CONSTRAINT IF NOT EXISTS fk_bid_transporter
FOREIGN KEY (transporter_id) REFERENCES transporter(transporter_id) ON DELETE CASCADE;

-- Booking table foreign keys
ALTER TABLE booking
ADD CONSTRAINT IF NOT EXISTS fk_booking_load
FOREIGN KEY (load_id) REFERENCES load(load_id) ON DELETE CASCADE;

ALTER TABLE booking
ADD CONSTRAINT IF NOT EXISTS fk_booking_bid
FOREIGN KEY (bid_id) REFERENCES bid(bid_id) ON DELETE CASCADE;

ALTER TABLE booking
ADD CONSTRAINT IF NOT EXISTS fk_booking_transporter
FOREIGN KEY (transporter_id) REFERENCES transporter(transporter_id) ON DELETE CASCADE;

-- 3. Additional indexes for performance (if not created by Hibernate)
-- These are defined in @Table(indexes={...}) but listed here for reference

-- Load indexes
CREATE INDEX IF NOT EXISTS idx_load_status ON load(status);
CREATE INDEX IF NOT EXISTS idx_load_shipper ON load(shipper_id);
CREATE INDEX IF NOT EXISTS idx_load_date_posted ON load(date_posted);

-- Bid indexes
CREATE INDEX IF NOT EXISTS idx_bid_load ON bid(load_id);
CREATE INDEX IF NOT EXISTS idx_bid_transporter ON bid(transporter_id);
CREATE INDEX IF NOT EXISTS idx_bid_status ON bid(status);
CREATE INDEX IF NOT EXISTS idx_bid_submitted ON bid(submitted_at);

-- Booking indexes
CREATE INDEX IF NOT EXISTS idx_booking_load ON booking(load_id);
CREATE INDEX IF NOT EXISTS idx_booking_transporter ON booking(transporter_id);
CREATE INDEX IF NOT EXISTS idx_booking_bid ON booking(bid_id);
CREATE INDEX IF NOT EXISTS idx_booking_status ON booking(status);
CREATE INDEX IF NOT EXISTS idx_booking_date ON booking(booked_at);

-- ============================================
-- Verification Queries
-- ============================================
-- Run these to verify the constraints are in place:

-- Check indexes
SELECT indexname, tablename
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- Check foreign keys
SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name AS foreign_table_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
ORDER BY tc.table_name;