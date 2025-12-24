### 🇺🇸 English Version

## 🗳️ Online E-Voting System (Technical Documentation)

This project implements a secure electronic voting system based on **PKI (Public Key Infrastructure)** technology, utilizing the **Bouncy Castle** library.

### 🚀 Execution Order

1. **`SetupPKI.java`** (Run only once)
* Generates the **Root CA**.
* Generates **Organizer CA** and **Voter CA** (signed by the Root CA).
* Creates `.p12` files for these authorities in the project root.


2. **`MainMenu.java`** (Main Application)
* **Registration:** Automatic issuance of a certificate to the user.
* **Login:** Authentication via the `.p12` file and a password.



### 🛡️ Key Features

* **Two-Factor Authentication (2FA):** Requires both a digital certificate (file) and a password.
* **Automatic CRL (Revocation List):** The certificate is revoked after **3 failed login attempts**.
* **Integrity:** Prevents double voting by checking the username within each poll.
* **Role Management:** The system recognizes access levels based on the certificate **Issuer**.

---

### 🇷🇸 Verzija na srpskom jeziku

## 🗳️ Sistem za Online Glasanje (Tehnička Dokumentacija)

Ovaj projekat implementira siguran sistem za elektronsko glasanje zasnovan na **PKI (Public Key Infrastructure)** tehnologiji, koristeći **Bouncy Castle** biblioteku.

### 🚀 Redoslijed pokretanja

1. **`SetupPKI.java`** (Pokreni samo jednom)
* Generiše **Root CA** (Koreni sertifikat).
* Generiše **Organizator CA** i **Glasač CA** (potpisani od strane Root-a).
* Kreira `.p12` fajlove za ove autoritete u korenu projekta.


2. **`MainMenu.java`** (Glavna aplikacija)
* **Registracija:** Automatsko izdavanje sertifikata korisniku.
* **Login:** Autentifikacija putem `.p12` fajla i lozinke.



### 🛡️ Ključne funkcionalnosti

* **Dvo-faktorska autentifikacija (2FA):** Neophodan je digitalni sertifikat (fajl) i lozinka.
* **Automatski CRL (Lista povlačenja):** Sertifikat se povlači nakon **3 neuspešna pokušaja** prijave.
* **Integritet:** Sprečeno je duplo glasanje proverom korisničkog imena unutar svake ankete.
* **Uloge:** Sistem prepoznaje nivo pristupa na osnovu izdavača (**Issuer**) sertifikata.

---



