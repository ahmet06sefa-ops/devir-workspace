#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v10.82 — Günlük Asistan Ayarlar Ekranı (fragment_settings.xml) Sadeleştirici & Yeniden Yapılandırıcı.
8 Tematik Bölüm Alt Başlığı (Section Sub-Headers) ve 58 Satır (40 mevcut + 18 yeni atölye) üretir.
"""

def make_divider():
    return """                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:alpha="0.25"
                    android:background="?attr/colorOutlineVariant" />"""

def make_toggle(row_id, sw_id, alt_id, icon, str_title, str_alt):
    return f"""                <LinearLayout
                    android:id="@+id/{row_id}"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:background="?attr/selectableItemBackground"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:padding="14dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="{icon}"
                        android:textSize="@dimen/ga_yazi_dev" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="14dp"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:fontFamily="@font/poppins_semibold"
                            android:text="@string/{str_title}"
                            android:textColor="?attr/colorOnSurface"
                            android:textSize="@dimen/ga_yazi_normal" />

                        <TextView
                            android:id="@+id/{alt_id}"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="@string/{str_alt}"
                            android:textColor="?attr/colorOnSurfaceVariant"
                            android:textSize="@dimen/ga_yazi_kucuk" />
                    </LinearLayout>

                    <com.google.android.material.switchmaterial.SwitchMaterial
                        android:id="@+id/{sw_id}"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:clickable="false"
                        android:focusable="false" />
                </LinearLayout>"""

def make_row(row_id, sub_id, icon, str_title, str_sub, badge_id=None, switch_id=None):
    badge_xml = ""
    if badge_id:
        badge_xml = f"""
                    <TextView
                        android:id="@+id/{badge_id}"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginEnd="6dp"
                        android:background="@drawable/bg_chip_caramel"
                        android:paddingStart="8dp"
                        android:paddingTop="2dp"
                        android:paddingEnd="8dp"
                        android:paddingBottom="2dp"
                        android:text=""
                        android:textColor="?attr/colorOnPrimary"
                        android:textSize="@dimen/ga_yazi_mini"
                        android:visibility="gone" />"""
    switch_xml = ""
    if switch_id:
        switch_xml = f"""
                    <com.google.android.material.materialswitch.MaterialSwitch
                        android:id="@+id/{switch_id}"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginEnd="8dp"
                        android:clickable="false"
                        android:focusable="false" />"""
    sub_attr = f'android:text="@string/{str_sub}"' if str_sub else 'android:text=""'
    return f"""                <LinearLayout
                    android:id="@+id/{row_id}"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:background="?attr/selectableItemBackground"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:padding="14dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="{icon}"
                        android:textSize="@dimen/ga_yazi_dev" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="14dp"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:fontFamily="@font/poppins_semibold"
                            android:text="@string/{str_title}"
                            android:textColor="?attr/colorOnSurface"
                            android:textSize="@dimen/ga_yazi_normal" />

                        <TextView
                            android:id="@+id/{sub_id}"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            {sub_attr}
                            android:textColor="?attr/colorOnSurfaceVariant"
                            android:textSize="@dimen/ga_yazi_kucuk" />
                    </LinearLayout>{badge_xml}{switch_xml}

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="›"
                        android:textColor="?attr/colorOnSurfaceVariant"
                        android:textSize="@dimen/ga_yazi_buyuk" />
                </LinearLayout>"""

def make_section(str_res, top_margin="24dp"):
    return f"""        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="{top_margin}"
            android:fontFamily="@font/poppins_semibold"
            android:text="@string/{str_res}"
            android:textColor="?attr/colorPrimary"
            android:textSize="@dimen/ga_yazi_kucuk" />"""

def make_card(rows_list):
    content = "\n".join(rows_list)
    return f"""        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            app:cardCornerRadius="@dimen/ga_kose_buyuk"
            app:cardElevation="1dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical">
{content}
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>"""

def build_settings_xml():
    xml = []
    xml.append("""<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp"
        android:paddingTop="16dp"
        android:paddingBottom="32dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:fontFamily="@font/poppins_semibold"
            android:text="@string/settings_title"
            android:textColor="?attr/colorOnSurface"
            android:textSize="@dimen/ga_yazi_dev" />""")

    # 1. HIZLI KONTROLLER & TEMEL SEÇİMLER
    xml.append(make_section("asy_sec_hizli_kontroller", "16dp"))
    c1 = [
        make_toggle("rowGlassmorphismToggle", "swGlassmorphism", "txtGlassmorphismAlt", "✨", "gcm_setting_satir", "gcm_setting_alt"),
        make_divider(),
        make_toggle("rowAnaEkranButonToggle", "swAnaEkranButon", "txtAnaEkranButonAlt", "⚡", "ae_atolye_goster_baslik", "ae_atolye_goster_alt_kapali"),
        make_divider(),
        make_toggle("rowNamazAylikToggle", "swNamazAylik", "txtNamazAylikAlt", "🕌", "nay_setting_satir", "nay_setting_alt"),
        make_divider(),
        make_toggle("rowMotivasyonMansetToggle", "swMotivasyonManset", "txtMotivasyonMansetAlt", "🌟", "mmn_setting_satir", "mmn_setting_alt"),
        make_divider(),
        make_toggle("rowKpssModuToggle", "swKpssModu", "txtKpssModuAlt", "🎓", "kmy_setting_satir", "kmy_setting_alt"),
        make_divider(),
        make_toggle("rowTabloBaslikToggle", "swTabloBaslik", "txtTabloBaslikAlt", "📑", "tb_ayar_baslik", "tb_ayar_alt_kapali"),
        make_divider(),
        make_row("rowBinMaddeAtolye", "rowBinMaddeAtolyeSub", "📋", "bma_setting_satir", "bma_setting_alt")
    ]
    xml.append(make_card(c1))

    # 2. GÖRÜNÜM, TEMA & KİŞİSELLEŞTİRME
    xml.append(make_section("asy_sec_gorunum_tema"))
    c2 = [
        make_row("rowTheme", "rowThemeSub", "🌓", "row_theme", "row_theme_sub"),
        make_divider(),
        make_row("rowWidgetTheme", "rowWidgetThemeSub", "🎨", "wt_row", "wt_row_sub"),
        make_divider(),
        make_row("rowAnaDuzen", "rowAnaDuzenSub", "📱", "ad_row", "ad_row_sub"),
        make_divider(),
        make_row("rowGorunum", "rowGorunumSub", "👁", "gr_row", "gr_row_sub"),
        make_divider(),
        make_row("rowTasarimAtolye", "rowTasarimAtolyeSub", "📐", "ta_setting_satir", "ta_setting_alt"),
        make_divider(),
        make_row("rowSounds", "rowSoundsSub", "🎧", "row_sounds", "row_sounds_sub"),
        make_divider(),
        make_row("rowSayacAyar", "rowSayacAyarSub", "⏱", "asy_row_sayac_ayar", "asy_row_sayac_ayar_sub"),
        make_divider(),
        make_row("rowWidgetFiltre", "rowWidgetFiltreSub", "🎛", "asy_row_widget_filtre", "asy_row_widget_filtre_sub"),
        make_divider(),
        make_row("rowWidgetTema", "rowWidgetTemaSub", "🖌", "asy_row_widget_tema", "asy_row_widget_tema_sub")
    ]
    xml.append(make_card(c2))

    # 3. YAPAY ZEKÂ, KOÇLUK & OTONOM ASİSTAN
    xml.append(make_section("asy_sec_yapay_zeka"))
    c3 = [
        make_row("rowAi", "rowAiSub", "🤖", "row_ai", "row_ai_sub_off"),
        make_divider(),
        make_row("rowKoc", "rowKocSub", "🦉", "koc_row", "koc_row_sub"),
        make_divider(),
        make_row("rowAkilliGundemMerkezi", "rowAkilliGundemMerkeziSub", "🌅", "agm_setting_satir", "agm_setting_alt"),
        make_divider(),
        make_row("rowEvrenselOtonomMerkez", "rowEvrenselOtonomMerkezSub", "👑", "eom_setting_satir", "eom_setting_alt"),
        make_divider(),
        make_row("rowOtonomMerkez", "rowOtonomMerkezSub", "⚡", "ot_setting_satir", "ot_setting_alt"),
        make_divider(),
        make_row("rowManuelKontrol", "rowManuelKontrolSub", "🎛", "mk_setting_satir", "mk_setting_alt"),
        make_divider(),
        make_row("rowSohbetGecmisi", "rowSohbetGecmisiSub", "💬", "asy_row_sohbet_gecmisi", "asy_row_sohbet_gecmisi_sub"),
        make_divider(),
        make_row("rowOgretmen", "rowOgretmenSub", "👨‍🏫", "asy_row_ogretmen", "asy_row_ogretmen_sub")
    ]
    xml.append(make_card(c3))

    # 4. KONULARIM, ÇALIŞMA & İLERLEME ATÖLYELERİ
    xml.append(make_section("asy_sec_konularim_ders"))
    c4 = [
        make_row("rowOgrenme", "rowOgrenmeSub", "🌱", "og_row", "og_row_sub"),
        make_divider(),
        make_row("rowKonuTekrar", "rowKonuTekrarSub", "🔁", "kt_row", "kt_row_sub", switch_id="svicKonuTekrar"),
        make_divider(),
        make_row("rowSozluk", "rowSozlukSub", "📖", "sz_row", "sz_row_sub"),
        make_divider(),
        make_row("rowKanit", "rowKanitSub", "📷", "kn_row", "kn_row_sub"),
        make_divider(),
        make_row("rowHatalarim", "rowHatalarimSub", "🎯", "ht_row", "ht_row_sub"),
        make_divider(),
        make_row("rowTakip", "rowTakipSub", "⚡", "tk_row", "tk_row_sub", badge_id="rowTakipRozet"),
        make_divider(),
        make_row("rowKarne", "rowKarneSub", "📊", "vk_setting_satir", "vk_setting_alt"),
        make_divider(),
        make_row("rowDersKolaylik", "rowDersKolaylikSub", "🎒", "dk_setting_satir", "dk_setting_alt"),
        make_divider(),
        make_row("rowDersIleriFaz", "rowDersIleriFazSub", "🧠", "df_setting_satir", "df_setting_alt"),
        make_divider(),
        make_row("rowDersUzmanMerkez", "rowDersUzmanMerkezSub", "🔬", "df5_setting_satir", "df5_setting_alt"),
        make_divider(),
        make_row("rowDersUzmanFaz6", "rowDersUzmanFaz6Sub", "🛡", "df6_setting_satir", "df6_setting_alt"),
        make_divider(),
        make_row("rowGelismiAtolye", "rowGelismiAtolyeSub", "⚙", "cdej_setting_satir", "cdej_setting_alt"),
        make_divider(),
        make_row("rowUzmanModuller", "rowUzmanModullerSub", "🏆", "uz_setting_satir", "uz_setting_alt"),
        make_divider(),
        make_row("rowKpssMerkeziYonetim", "txtKpssDurum", "🎓", "kmy_setting_yonetim_satir", "kmy_setting_yonetim_alt"),
        make_divider(),
        make_row("rowAnalitik", "rowAnalitikSub", "📈", "asy_row_analitik", "asy_row_analitik_sub"),
        make_divider(),
        make_row("rowHaftaPlan", "rowHaftaPlanSub", "🗓", "asy_row_hafta_plan", "asy_row_hafta_plan_sub"),
        make_divider(),
        make_row("rowFlasKart", "rowFlasKartSub", "🗂", "asy_row_flas_kart", "asy_row_flas_kart_sub"),
        make_divider(),
        make_row("rowSoruCoz", "rowSoruCozSub", "💡", "asy_row_soru_coz", "asy_row_soru_coz_sub"),
        make_divider(),
        make_row("rowPdfArama", "rowPdfAramaSub", "🔍", "asy_row_pdf_arama", "asy_row_pdf_arama_sub")
    ]
    xml.append(make_card(c4))

    # 5. YAŞAM SAĞLIĞI, MEDİKAL & İBADET YÖNETİMİ
    xml.append(make_section("asy_sec_yasam_saglik"))
    c5 = [
        make_row("rowNamazAylikYonetim", "rowNamazAylikYonetimSub", "🕌", "nay_baslik", "nay_setting_alt"),
        make_divider(),
        make_row("rowSaglik", "rowSaglikSub", "💧", "sy_satir", "sy_satir_alt"),
        make_divider(),
        make_row("rowYasamModulleri", "rowYasamModulleriSub", "🌱", "ym_setting_satir", "ym_setting_alt"),
        make_divider(),
        make_row("rowYasamSaglikFinans", "rowYasamSaglikFinansSub", "⚖", "ysf2_setting_satir", "ysf2_setting_alt"),
        make_divider(),
        make_row("rowYasamSaglikFinansFaz3", "rowYasamSaglikFinansFaz3Sub", "🆘", "ysf3_setting_satir", "ysf3_setting_alt"),
        make_divider(),
        make_row("rowUyku", "rowUykuSub", "🌙", "uy_row", "uy_row_sub"),
        make_divider(),
        make_row("rowPlanSabah", "planSabahDurum", "🌅", "w42_satir_sabah", "w42_satir_sabah_alt"),
        make_divider(),
        make_row("rowPlanAksam", "planAksamDurum", "🌙", "w42_satir_aksam", "w42_satir_aksam_alt"),
        make_divider(),
        make_row("rowNamazAyar", "rowNamazAyarSub", "🔔", "asy_row_namaz_ayar", "asy_row_namaz_ayar_sub"),
        make_divider(),
        make_row("rowNefes", "rowNefesSub", "🍃", "asy_row_nefes", "asy_row_nefes_sub"),
        make_divider(),
        make_row("rowMikroGunluk", "rowMikroGunlukSub", "📝", "asy_row_mikro_gunluk", "asy_row_mikro_gunluk_sub"),
        make_divider(),
        make_row("rowFilm", "rowFilmSub", "🎬", "asy_row_film", "asy_row_film_sub")
    ]
    xml.append(make_card(c5))

    # 6. BİLDİRİMLER, ODAK KİLİDİ & TAKVİM
    xml.append(make_section("asy_sec_bildirim_alarm"))
    c6 = [
        make_row("rowNotifications", "rowNotificationsSub", "🔔", "row_notif", "row_notif_sub"),
        make_divider(),
        make_row("rowKilit", "rowKilitSub", "🔒", "w22_satir", "w22_satir_alt"),
        make_divider(),
        make_row("rowTakvim", "rowTakvimSub", "📅", "tk_satir", "tk_satir_alt"),
        make_divider(),
        make_row("rowBildirimTani", "rowBildirimTaniSub", "🛠", "asy_row_bildirim_tani", "asy_row_bildirim_tani_sub"),
        make_divider(),
        make_row("rowOnlineBekci", "rowOnlineBekciSub", "🌐", "asy_row_online_bekci", "asy_row_online_bekci_sub")
    ]
    xml.append(make_card(c6))

    # 7. DEPOLAMA, YEDEKLEME & SİSTEM TEŞHİS
    xml.append(make_section("asy_sec_depolama_sistem"))
    c7 = [
        make_row("rowSync", "rowSyncSub", "🔄", "row_sync", "w37_satir_alt"),
        make_divider(),
        make_row("rowBackup", "rowBackupSub", "📤", "row_backup", "row_backup_sub"),
        make_divider(),
        make_row("rowRestore", "rowRestoreSub", "📥", "row_restore", "row_restore_sub"),
        make_divider(),
        make_row("rowDepolama", "rowDepolamaSub", "📦", "dp_row", "dp_row_sub"),
        make_divider(),
        make_row("rowSistem", "rowSistemSub", "🔧", "gcm_setting_satir", "gcm_setting_alt", "rowSistemRozet"),
        make_divider(),
        make_row("rowArsiv", "rowArsivSub", "🗃", "asy_row_arsiv", "asy_row_arsiv_sub"),
        make_divider(),
        make_row("rowSeneFilmi", "rowSeneFilmiSub", "🎞", "asy_row_sene_filmi", "asy_row_sene_filmi_sub")
    ]
    xml.append(make_card(c7))

    # 8. HAKKINDA & SÜRÜM
    xml.append(make_section("asy_sec_hakkinda"))
    c8 = [
        make_row("rowAbout", "versionText", "ℹ", "row_about", "")
    ]
    xml.append(make_card(c8))

    xml.append("""    </LinearLayout>
</ScrollView>
""")

    return "\n".join(xml)

if __name__ == "__main__":
    content = build_settings_xml()
    with open("/home/user/GunlukAsistan/app/src/main/res/layout/fragment_settings.xml", "w", encoding="utf-8") as f:
        f.write(content)
    print("BASARILI: fragment_settings.xml uretildi.")
