-- Supabase Temiz Kurulum Şeması (Blind Social)
-- Supabase Güvenli Şema (Blind Social)
-- Mevcut verileri KORUR, sadece gerekli tabloları/sütunları oluşturur.

-- 1. Profiller Tablosu
CREATE TABLE IF NOT EXISTS public.profiller (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    email TEXT NOT NULL,
    kullanici_adi TEXT NOT NULL UNIQUE,
    ad TEXT,
    soyad TEXT,
    dogum_tarihi TEXT,
    hakkimda TEXT,
    baglantilar TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Profil tablosuna sonradan eklenen sütunlar (Mevcut veritabanları için güncelleme komutları)
ALTER TABLE public.profiller DROP COLUMN IF EXISTS ad_soyad;
ALTER TABLE public.profiller ADD COLUMN IF NOT EXISTS ad TEXT;
ALTER TABLE public.profiller ADD COLUMN IF NOT EXISTS soyad TEXT;
ALTER TABLE public.profiller ADD COLUMN IF NOT EXISTS hakkimda TEXT;
ALTER TABLE public.profiller ADD COLUMN IF NOT EXISTS baglantilar TEXT;

-- Profiller için güvenlik kalkanı (RLS)
ALTER TABLE public.profiller ENABLE ROW LEVEL SECURITY;
-- Herkese profil okuma izni
CREATE POLICY "Profilleri herkes görebilir" ON public.profiller FOR SELECT USING (true);
-- Sadece kişi kendi profilini yaratabilir/güncelleyebilir
CREATE POLICY "Kullanıcı sadece kendi profilini değiştirebilir" ON public.profiller FOR ALL USING (auth.uid() = id);

-- 3. Odalar Tablosu
CREATE TABLE IF NOT EXISTS public.odalar (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    oda_adi TEXT NOT NULL,
    kapasite INTEGER NOT NULL DEFAULT 3,
    kategori TEXT NOT NULL DEFAULT 'Genel',
    sifre TEXT, -- Boşsa null olur (şifresiz)
    kurucu_id UUID REFERENCES public.profiller(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.odalar ENABLE ROW LEVEL SECURITY;
-- Oturum açmış herkes odaları listeleyebilir
CREATE POLICY "Odaları oturum açanlar görebilir" ON public.odalar FOR SELECT USING (auth.role() = 'authenticated');
-- Sadece oturum açmış kullanıcılar oda oluşturabilir
CREATE POLICY "Odaları oturum açanlar oluşturabilir" ON public.odalar FOR INSERT WITH CHECK (auth.role() = 'authenticated');
-- Odayı sadece kurucusu güncelleyebilir veya silebilir
CREATE POLICY "Odayı kurucu düzenleyebilir" ON public.odalar FOR UPDATE USING (auth.uid() = kurucu_id);
CREATE POLICY "Odayı kurucu silebilir" ON public.odalar FOR DELETE USING (auth.uid() = kurucu_id);

-- 4. Mesajlar Tablosu
CREATE TABLE IF NOT EXISTS public.mesajlar (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    oda_id UUID REFERENCES public.odalar(id) ON DELETE CASCADE NOT NULL,
    kullanici_id UUID REFERENCES public.profiller(id) ON DELETE CASCADE NOT NULL,
    metin TEXT NOT NULL,
    mesaj_tipi TEXT NOT NULL DEFAULT 'metin', -- 'metin' veya 'ses'
    olusturma_tarihi TIMESTAMPTZ DEFAULT now(),
    gonderen_kullanici_adi TEXT -- Performans için (JOIN yerine düz metin olarak tutulabilir)
);

ALTER TABLE public.mesajlar ENABLE ROW LEVEL SECURITY;
-- Odadaki mesajları oturum açanlar görebilir
CREATE POLICY "Mesajları oturum açanlar görebilir" ON public.mesajlar FOR SELECT USING (auth.role() = 'authenticated');
-- Mesajları sadece oturum açmış kişiler gönderebilir
CREATE POLICY "Mesaj gönderme" ON public.mesajlar FOR INSERT WITH CHECK (auth.uid() = kullanici_id);
-- Sadece mesajı gönderen veya odanın kurucusu mesajı silebilir
CREATE POLICY "Mesaj silme" ON public.mesajlar FOR DELETE USING (
    auth.uid() = kullanici_id OR
    auth.uid() = (SELECT kurucu_id FROM public.odalar WHERE id = mesajlar.oda_id LIMIT 1)
);

-- 5. BS Meydan (Mikroblog) Tabloları
CREATE TABLE IF NOT EXISTS public.gonderiler (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    yazar_id UUID REFERENCES public.profiller(id) ON DELETE CASCADE NOT NULL,
    icerik TEXT NOT NULL,
    begeni_sayisi INTEGER DEFAULT 0,
    yorum_sayisi INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.gonderiler ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Gönderileri herkes görebilir" ON public.gonderiler FOR SELECT USING (true);
CREATE POLICY "Gönderi oluşturma" ON public.gonderiler FOR INSERT WITH CHECK (auth.uid() = yazar_id);
CREATE POLICY "Gönderi silme" ON public.gonderiler FOR DELETE USING (auth.uid() = yazar_id);
CREATE POLICY "Gönderi güncelleme" ON public.gonderiler FOR UPDATE USING (auth.uid() = yazar_id);

CREATE TABLE IF NOT EXISTS public.begeniler (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    gonderi_id UUID REFERENCES public.gonderiler(id) ON DELETE CASCADE NOT NULL,
    kullanici_id UUID REFERENCES public.profiller(id) ON DELETE CASCADE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(gonderi_id, kullanici_id)
);

ALTER TABLE public.begeniler ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Beğenileri herkes görebilir" ON public.begeniler FOR SELECT USING (true);
CREATE POLICY "Beğeni yapma" ON public.begeniler FOR INSERT WITH CHECK (auth.uid() = kullanici_id);
CREATE POLICY "Beğeni geri alma" ON public.begeniler FOR DELETE USING (auth.uid() = kullanici_id);

CREATE TABLE IF NOT EXISTS public.yorumlar (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    gonderi_id UUID REFERENCES public.gonderiler(id) ON DELETE CASCADE NOT NULL,
    yazar_id UUID REFERENCES public.profiller(id) ON DELETE CASCADE NOT NULL,
    icerik TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.yorumlar ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Yorumları herkes görebilir" ON public.yorumlar FOR SELECT USING (true);
CREATE POLICY "Yorum yapma" ON public.yorumlar FOR INSERT WITH CHECK (auth.uid() = yazar_id);
CREATE POLICY "Yorum silme" ON public.yorumlar FOR DELETE USING (auth.uid() = yazar_id);

-- Beğeni ve Yorum Sayılarını Otomatik Güncelleyen Triggerlar
CREATE OR REPLACE FUNCTION public.update_gonderi_begeni_sayisi()
RETURNS trigger AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.gonderiler SET begeni_sayisi = begeni_sayisi + 1 WHERE id = NEW.gonderi_id;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.gonderiler SET begeni_sayisi = begeni_sayisi - 1 WHERE id = OLD.gonderi_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_begeni_degisikligi ON public.begeniler;
CREATE TRIGGER on_begeni_degisikligi
  AFTER INSERT OR DELETE ON public.begeniler
  FOR EACH ROW EXECUTE PROCEDURE public.update_gonderi_begeni_sayisi();

CREATE OR REPLACE FUNCTION public.update_gonderi_yorum_sayisi()
RETURNS trigger AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.gonderiler SET yorum_sayisi = yorum_sayisi + 1 WHERE id = NEW.gonderi_id;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.gonderiler SET yorum_sayisi = yorum_sayisi - 1 WHERE id = OLD.gonderi_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_yorum_degisikligi ON public.yorumlar;
CREATE TRIGGER on_yorum_degisikligi
  AFTER INSERT OR DELETE ON public.yorumlar
  FOR EACH ROW EXECUTE PROCEDURE public.update_gonderi_yorum_sayisi();

-- Ek: Profil tablosu ile otomatik eşleşen Trigger
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiller (id, email, kullanici_adi, ad, soyad)
  VALUES (
    new.id,
    new.email,
    new.raw_user_meta_data->>'username',
    new.raw_user_meta_data->>'ad',
    new.raw_user_meta_data->>'soyad'
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- 5. Realtime Bildirimlerini Aç
-- 6. Realtime Bildirimlerini Aç
-- Tablolardaki değişiklikleri anlık olarak dinlemek (Flow/Websocket) için gerekli.
alter publication supabase_realtime add table public.mesajlar;
alter publication supabase_realtime add table public.odalar;
alter publication supabase_realtime add table public.gonderiler;
alter publication supabase_realtime add table public.begeniler;
alter publication supabase_realtime add table public.yorumlar;

-- 7. Storage (Dosya Depolama) - Sesli Mesajlar Bucket'ı
-- Eğer bucket yoksa ekler (Postgres üzerinden storage ekleme scripti)
INSERT INTO storage.buckets (id, name, public) VALUES ('sesli_mesajlar', 'sesli_mesajlar', true)
ON CONFLICT (id) DO NOTHING;

-- Storage için RLS ayarları (Kayıt okuma ve yazma)
CREATE POLICY "Sesli mesajları herkes okuyabilir" ON storage.objects FOR SELECT USING (bucket_id = 'sesli_mesajlar');
CREATE POLICY "Oturum açanlar sesli mesaj yükleyebilir" ON storage.objects FOR INSERT WITH CHECK (
    bucket_id = 'sesli_mesajlar' AND auth.role() = 'authenticated'
);
CREATE POLICY "Kullanıcılar kendi ses kayıtlarını silebilir" ON storage.objects FOR DELETE USING (
    bucket_id = 'sesli_mesajlar' AND auth.uid() = owner
);