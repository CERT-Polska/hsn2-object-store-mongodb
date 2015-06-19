
all:	hsn2-object-store-mongodb-package

clean:	hsn2-object-store-mongodb-package-clean

hsn2-object-store-mongodb-package:
	mvn clean install -U -Pbundle
	mkdir -p build/hsn2-object-store-mongodb
	tar xzf target/hsn2-object-store-mongodb-1.0.0-SNAPSHOT.tar.gz -C build/hsn2-object-store-mongodb

hsn2-object-store-mongodb-package-clean:
	rm -rf build

build-local:
	mvn clean install -U -Pbundle