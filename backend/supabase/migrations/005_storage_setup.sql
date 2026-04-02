-- ============================================================
-- Raqeem — 005_storage_setup.sql
-- Supabase Storage bucket for receipt images.
-- ============================================================

-- Create receipts bucket (private)
INSERT INTO storage.buckets (id, name, public)
VALUES ('receipts', 'receipts', false);

-- RLS: only the owner can access files in their folder
-- File path convention: receipts/{user_id}/{transaction_id}.jpg
CREATE POLICY "owner_access_receipts" ON storage.objects
  FOR ALL USING (
    bucket_id = 'receipts' AND
    auth.uid()::text = (storage.foldername(name))[1]
  )
  WITH CHECK (
    bucket_id = 'receipts' AND
    auth.uid()::text = (storage.foldername(name))[1]
  );
