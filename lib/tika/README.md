# Tika jar acquisition steps

The following steps can be used to generate the contents of this
directory:

``` shell
PATH_TO_FITS=
TIKA_VERSION=

cd /var/tmp
git clone git@github.com:apache/tika.git
cd tika/tika-bundles/tika-bundle-standard
git checkout $TIKA_VERSION
mvn dependency:copy-dependencies -DincludeScope=runtime
rm "${PATH_TO_FITS}/lib/tika/*.jar"
cp target/dependency/* "${PATH_TO_FITS}/lib/tika"
rm "${PATH_TO_FITS}/lib/tika/slf4j*"
```
