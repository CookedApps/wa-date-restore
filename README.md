# WhatsApp image date restore
A simple Java program for restoring the creation / modification date of WhatApp images and videos by their filename.

I ran into an issue where all original dates of my WhatsApp images where lost after restoring them from Google Drive Backup. 
This tiny program lets you restore these dates by extracting the original date from the filename. 
The filenames seemingly follow a specific pattern `IMG-<date>-WA<number>.<extension>` (e.g. IMG-20160315-WA0020.jpg).