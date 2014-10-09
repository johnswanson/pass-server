# pw

A tiny clojure/cljs server to act as a web front-end for [pass](http://www.passwordstore.org/).
And also to play around with clojure and clojurescript. :)

## Motivation

Pass is awesome. The one thing that scares me is the fear of losing my HD and not having backups.
So then I decided to backup my gnupg keys and .password-store with Tarsnap. Except, wait, then I
am still relying on having local backups of my keyfile for Tarsnap. I know--I'll put them on S3.
Except, wait, either I make them public and encrypt them (which gives an attacker a lot of time
to brute force my key), or I make them private... and then I am relying on having my AWS keys
available. Ok, I'll just use Dropbox. Except I'm uncomfortable giving Dropbox access to any of
this. Ok, I'll put it on my server somewhere. Except I only allow key-based authentication, so
now I need to back up my key.

Hmm.

In the end, I decided to build a super simple front-end for pass. You send in a service and a
password, and if you entered a correct pair, it will send back the encrypted password.

(Not that I don't back stuff up. But once you start thinking about what you'd do if you lost
all your passwords, it's hard not to get excessively paranoid.)

## Security Considerations

- You'd be an idiot if you ran this over HTTP! :) I am running this behind Nginx over HTTPS.
- Don't make this a target for brute forcing. Nginx comes in handy again for rate limiting.

## Usage

### Dev

First, create and edit profiles.clj. It should contain the following:

	{:dev {:source-paths ["dev"]
		   :dependencies [[org.clojure/tools.namespace "0.2.7"]]
		   :env {:server-port 8081
				 :password-store "/path/to/.password-store"}}}

Then run:

    lein cljsbuild auto
	lein repl

In the repl, run `(reset)` to launch the server, or relaunch it after editing code. If there's an error,
`reset` may be out of scope, in which case, run `(refresh)`. This is Stuart Sierra's [Reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)
workflow.

### Production

	lein clean
	lein uberjar
	lein cljsbuild once prod
	SERVER_PORT=8081 PASSWORD_STORE=/path/to/.password-store IS_PRODUCTION=true java -jar target/pw-0.1.0-SNAPSHOT-standalone.jar


## Sample Nginx Conf

	# redirect HTTP to HTTPS
    server {
        listen 80;
        server_name your.hostname.here;
        return 301 https://$host$request_uri$is_args$args;
    }
	# upstream clojure server
    upstream eightyeightyone {
        server 127.0.0.1:8081 fail_timeout=0;
    }
	limit_req_zone $binary_remote_addr zone=one:10m rate=30r/m; # rate limiting
	server {
		listen 443 ssl;
		ssl_certificate /etc/nginx/ssl-cert/less.sexy.crt;
		ssl_certificate_key /etc/nginx/ssl-cert/less.sexy.key;
		root /path/to/repo/resources/public;
		server_name your.hostname.here;
		location / {
			# first try serving static from resources/public.
			# otherwise ask upstream clojure server.
			try_files $uri @eightyeightyone;
		}
		location @eightyeightyone {
			proxy_redirect off;
			proxy_buffering off;
			proxy_set_header Host $http_host;
			proxy_pass http://eightyeightyone;
			limit_req zone=one burst=5;
		}
	}

## License

Copyright 2014 John Swanson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
