# Ungrad fork of the elexis-3-core Repository

## Build:

Prerequisites:

* Java 8
* Maven > 3.2

```
git clone https://github.com/rgwch/elexis-3-core
cd elexis-3-core
./build.sh
```
## Download of precompiled binaries:

<http://www.elexis.ch/ungrad2018/products/elexis-core/latest/>

## Jenkins Build history

<http://www.elexis.ch:8080/>

## Upgrade database

If upgrading from ungrad (2016), read [here](http://www.elexis.ch/ungrad/reference/update_34/) (or try your luck after making a backup - it might run automatically).

If upgrading/transgrading from Legacy-Elexis: Apply the SQL Script `ch.elexis.core.data - rsc/apply_ungrad2018_db.sql`
This should not break compatibility. It just adds some ungrad specific extensions.

## Main differences to elexis/elexis-3-core

* Modified Login Dialog

* Case-insensitive usernames

* User with appropiate privileges can always modify case Details (Falldetails) 

* modified extinfo fields

* medication ui:
  * a bit more responsive
  * more user feed back on long running operations
  
* Artikelliste: 
   * Blackbox-Filter
   * substance filter
   * ability to import only pharma or non-pharma articles or both.
   * display of contianed substances for pharma-articles.
  
* Database
    * ID fields changed to 36 Chars (RFC 4122 UUIDv4)
    * Time-Field in "Behandlungen" - allows for better sorting od same-day consultations
    * time field in Behandlungen.

## See also

<http://www.elexis.ch/ungrad>

<http://github.com/rgwch/elexis-3-base>

<http://github.com/rgwch/elexis-ungrad>
