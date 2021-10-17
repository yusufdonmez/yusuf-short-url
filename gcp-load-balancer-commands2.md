

### MEDIUM INSTRUCTION:
```sh
gcloud compute network-endpoint-groups create short-url  --network-endpoint-type="internet-fqdn-port" --global

gcloud compute network-endpoint-groups update short-url --add-endpoint="fqdn=short-url-gate-2zoj1r36.ew.gateway.dev	,port=443" --global

gcloud compute backend-services create short-url-backend --global --enable-cdn --protocol=HTTPS

gcloud compute backend-services update short-url-backend --custom-request-header "Host:short-url-gate-2zoj1r36.ew.gateway.dev" --global

gcloud compute backend-services add-backend short-url-backend --network-endpoint-group "short-url" --global-network-endpoint-group --global

gcloud compute url-maps create short-url-url-map --default-service short-url-backend --global

gcloud compute target-https-proxies create short-url-target-https-proxy  --url-map=short-url-url-map --ssl-certificates=godady-merged --global

gcloud compute forwarding-rules create short-url-forwarding-rule --ip-protocol=TCP  --ports=443   --global   --target-https-proxy=short-url-target-https-proxy

gcloud compute forwarding-rules list
```

---

### ShortUrl table create Mysql DB

```sql
CREATE TABLE ShortUrl (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  keyword varchar(200) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  url text CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY keyword (keyword)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=latin1
```