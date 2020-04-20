if [ ! -f keycloak-9.0.3.zip ]
then
    echo 'Downloading and unzipping keycloak setup files...'
    wget -nc https://downloads.jboss.org/keycloak/9.0.3/keycloak-9.0.3.zip
    unzip keycloak-9.0.3.zip
fi