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

Oh, plus there's the annoyance of not being able to copy and paste passwords from my phone.

Hmm.

In the end, I decided to build a super simple front-end for pass. You send in a service and a
password, and if you entered a correct pair, it will send back the encrypted password.

(Not that I don't back stuff up. But once you start thinking about what you'd do if you lost
all your passwords, it's hard not to get excessively paranoid.)

## Security Considerations

- You'd be an idiot if you ran this over HTTP! :) I am running this behind Nginx over HTTPS.
- Don't make this a target for brute forcing. Nginx comes in handy again for rate limiting.

## Usage

My goal here was to let Nginx take care of HTTPS and rate limiting. Both the dev server and
production servers will run on high ports (I've chosen 8082 and 8081, but choose as you will).
Nginx will take care of routing port 443 to the production server.

Configuration works through environmental variables. In development, you set them in
profiles.clj. In production, you make an uberjar and execute it with those environmental vars.

- `SERVER_PORT` is self-explanatory.
- `SERVER_HOST` is too. Just remember that 127.0.0.1 means we'll refuse any non-local connections.
- `PASSWORD_STORE` is the location of your `.password-store` directory. If you're using the defaults
for `pass`, that'll be `$HOME/.password-store`. But for development it might be a good idea to use
a fake one, since submitting your password over any port that Nginx isn't providing secure transport
for is bad news.
- `IS_PRODUCTION` is, well, this is not ideal. The server needs to know whether to serve the minified,
compacted, advanced-compiled production javascript or the development versions. So specifying this means
the server will try to serve production scripts; otherwise it'll serve dev versions. There is definitely
a better way to go about this, because this smells strongly. But it works for now.

### Dev

First, create and edit profiles.clj. It should contain the following:

	{:dev {:source-paths ["dev"]
		   :dependencies [[org.clojure/tools.namespace "0.2.7"]]
		   :env {:server-port 8082
				 :password-store "/path/to/.password-store"}}}

I'm probably repeating myself by now, but once again, *transport encryption is not provided here*!
So if you're accessing your real password store over port 8082 and entering your real master password,
you're revealing your password to anyone listening in. Use a fake password-store if you're accessing
the Jetty server directly, instead of through a secure intermediary.

Then run:

    lein cljsbuild once
	lein repl

In the repl, run `(reset)` to launch the server, or relaunch it after editing code. If there's an error,
`reset` may be out of scope, in which case, run `(refresh)`. This is Stuart Sierra's [Reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)
workflow.

### Production

We want to refuse any non-local connections, so the service can only be accessed by being forwarded
from an HTTPS connection on 443.

	lein clean
	lein cljsbuild once prod
	lein uberjar
	SERVER_PORT=8081 SERVER_HOST=127.0.0.1 PASSWORD_STORE=/path/to/.password-store IS_PRODUCTION=true java -jar target/pw-0.1.0-SNAPSHOT-standalone.jar


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
