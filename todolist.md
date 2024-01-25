things to do

Features
    -Google Places API
    -Main screen ui
        ~~map page~~
        profile page (work in progress)
            ~~-add users pins as box~~
            ~~-add users total likes~~
            ~~-add user pins~~
            ~~-click on pins to move on map~~
        search page
            ~~-search functionality~~
            ~~-recently created pins~~
            ~~-click pins to move to location on map~~
    -enhance pins
        ~~profile picture~~
        ~~tags~~
        ~~chat~~
        ~~likes~~
        ~~delete pins (only for the user)~~

Optimization and bug fixes:
    -check on delay between pin creation and pin showing up on map
    -better pin asset
    -deletion cleanup
        -picture removal
        -likes removal
    -clearing pin fields post creation
    -tags in pininfo get smushed in smaller screens
    -pins in profile not showing titles
    -search pin click doesn't open pininfodialog, add to nav host
    -logging out from profile page causes crash
    -don't allow empty fields for title and description in creation
        -buggy, sometimes allows?
    -if pin is in last 5 pins and deleted, it should be deleted from firebase collection of lastFivePins
    -alert messages for achievements
    -ERROR MESSAGES EVERYWHERE
~~-Google account login~~
~~look up ability to use google map functionality such as buisness info~~
~~google account login~~
~~username~~
~~Map Controls~~