# 🖥️ Setup MacBook — Data Engineer (Java / Spark / Kafka / Elasticsearch / IBM Cloud)

> Guide d'installation **complet et ordonné** pour un MacBook (Apple Silicon) en environnement bancaire :
> proxy HTTP d'entreprise, certificats internes, Nexus (Maven / PyPI / npm / Docker), GitLab, IntelliJ Community,
> Docker Desktop, IBM Cloud (Hadoop / Spark / Kafka / Elasticsearch) et **IA en local**.

---

## 📑 Sommaire

0. [Conventions & valeurs à remplacer](#0-conventions--valeurs-à-remplacer)
1. [Jour J : ordre d'installation](#1-jour-j--ordre-dinstallation)
2. [Proxy & certificats d'entreprise (À FAIRE EN PREMIER)](#2-proxy--certificats-dentreprise-à-faire-en-premier)
3. [Homebrew & Brewfile](#3-homebrew--brewfile)
4. [Shell : zsh, Oh My Zsh, `.zshrc` complet](#4-shell--zsh-oh-my-zsh-zshrc-complet)
5. [Git & GitLab](#5-git--gitlab)
6. [Java : SDKMAN, Maven, Gradle (via Nexus)](#6-java--sdkman-maven-gradle-via-nexus)
7. [Python : pyenv / uv / pip via Nexus PyPI](#7-python--pyenv--uv--pip-via-nexus-pypi)
8. [Node.js / npm via Nexus](#8-nodejs--npm-via-nexus)
9. [Docker Desktop (proxy, Nexus, Apple Silicon)](#9-docker-desktop-proxy-nexus-apple-silicon)
10. [Stack Big Data locale (Spark, Kafka, Elasticsearch, Hadoop)](#10-stack-big-data-locale)
11. [IBM Cloud CLI & Kubernetes / OpenShift](#11-ibm-cloud-cli--kubernetes--openshift)
12. [Vim / Neovim](#12-vim--neovim)
13. [IntelliJ IDEA Community : config + plugins](#13-intellij-idea-community--config--plugins)
14. [IA en local (Ollama, Open WebUI, IDE)](#14-ia-en-local)
15. [Autres outils utiles](#15-autres-outils-utiles)
16. [Réglages macOS](#16-réglages-macos)
17. [Script de vérification](#17-script-de-vérification)
18. [Dépannage proxy / SSL](#18-dépannage-proxy--ssl)
19. [Checklist finale](#19-checklist-finale)

---

## 0. Conventions & valeurs à remplacer

Toutes les valeurs ci-dessous sont des **placeholders**. Récupère les vraies auprès de l'équipe infra / poste de travail / RSSI.

| Placeholder | Signification | Exemple |
|---|---|---|
| `PROXY_HOST` | Proxy HTTP(S) d'entreprise | `proxy.mycorp.local` |
| `PROXY_PORT` | Port du proxy | `8080` |
| `NEXUS_URL` | URL de Nexus | `https://nexus.mycorp.local` |
| `NEXUS_DOCKER` | Registre Docker proxy dans Nexus | `nexus.mycorp.local:8443` |
| `GITLAB_HOST` | GitLab interne | `gitlab.mycorp.local` |
| `CORP_CA` | Certificat(s) racine(s) d'entreprise (`.pem`/`.crt`) | `corp-root-ca.pem` |
| `NO_PROXY_LIST` | Domaines internes à ne pas proxifier | `localhost,127.0.0.1,.mycorp.local,...` |

> 🔐 **Règle d'or** : aucun mot de passe en clair dans un fichier versionné ni dans `.zshrc`.
> Les secrets vont dans `~/.secrets.zsh` (chmod 600, jamais commité) ou dans le **Trousseau macOS**.

> 🛡️ **Rappel conformité** : tout outil installé doit être conforme à la politique du poste (liste blanche, licences).
> En cas de doute (surtout plugins IA, extensions navigateur), valider avec le RSSI / l'équipe poste de travail.

---

## 1. Jour J : ordre d'installation

L'ordre compte : **sans proxy + certificats, rien ne télécharge**.

```text
 1. Récupérer : adresse proxy, PAC éventuel, certificats CA, URL Nexus, compte GitLab
 2. Installer le certificat CA d'entreprise dans le Trousseau (section 2)
 3. Exporter les variables proxy dans le terminal courant (section 2)
 4. Xcode Command Line Tools          → xcode-select --install
 5. Homebrew (si pas déjà là)          → section 3
 6. brew bundle (Brewfile)             → section 3
 7. Oh My Zsh + .zshrc                  → section 4
 8. Git + clé SSH + GitLab              → section 5
 9. SDKMAN + JDKs + Maven/Gradle        → section 6 (settings.xml Nexus)
10. pyenv/uv + pip.conf Nexus           → section 7
11. Docker Desktop (proxy + mirroir)    → section 9
12. IntelliJ + plugins + config         → section 13
13. IBM Cloud CLI + plugins             → section 11
14. Ollama + modèles                    → section 14
15. ./verify-setup.sh                   → section 17
```

---

## 2. Proxy & certificats d'entreprise (À FAIRE EN PREMIER)

### 2.1 Certificat CA dans le Trousseau macOS

```bash
# Récupère le(s) certificat(s) CA (fourni par l'IT, ou export depuis le navigateur)
mkdir -p ~/.certs
cp /chemin/vers/corp-root-ca.pem ~/.certs/

# Ajout au trousseau système (Safari, Chrome, curl système, Docker Desktop l'utilisent)
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain ~/.certs/corp-root-ca.pem
```

### 2.2 Bundle CA unique pour TOUS les outils (le point clé)

Chaque outil (curl Homebrew, Python, Node, Java, Git…) a son propre magasin de certificats.
On crée **un seul bundle** = CA publiques + CA d'entreprise, et on y pointe tout le monde.

```bash
# Après installation de Homebrew (section 3) :
brew install ca-certificates
cat "$(brew --prefix)/etc/ca-certificates/cert.pem" ~/.certs/*.pem > ~/.certs/ca-bundle.pem
```

Les variables qui pointent dessus sont dans le `.zshrc` (section 4) :
`SSL_CERT_FILE`, `REQUESTS_CA_BUNDLE`, `CURL_CA_BUNDLE`, `NODE_EXTRA_CA_CERTS`, `PIP_CERT`, `GIT_SSL_CAINFO`.

### 2.3 Proxy dans le terminal courant (bootstrap)

```bash
export http_proxy="http://PROXY_HOST:PROXY_PORT"
export https_proxy="$http_proxy"
export HTTP_PROXY="$http_proxy" HTTPS_PROXY="$http_proxy"
export no_proxy="localhost,127.0.0.1,::1,.local,.mycorp.local,GITLAB_HOST,NEXUS_URL_HOST,host.docker.internal,kubernetes.docker.internal"
export NO_PROXY="$no_proxy"
```

> 🔑 Proxy **authentifiant** (NTLM/Kerberos) ? La plupart des outils CLI gèrent mal NTLM.
> Solution classique : un proxy local relais (`brew install cntlm` ou `px` via pip) qui écoute sur `127.0.0.1:3128`,
> puis tu mets `http_proxy=http://127.0.0.1:3128` partout. À valider avec l'IT.

---

## 3. Homebrew & Brewfile

### 3.1 Homebrew

```bash
# Si pas déjà installé (Apple Silicon → /opt/homebrew)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
eval "$(/opt/homebrew/bin/brew shellenv)"

brew analytics off
brew update && brew doctor
```

### 3.2 `~/Brewfile`

Installe tout d'un coup : `brew bundle --file=~/Brewfile`

```ruby
# ============================ CLI de base ============================
brew "git"
brew "git-lfs"
brew "glab"                 # CLI GitLab
brew "gnupg"                # signature des commits
brew "pinentry-mac"
brew "coreutils"
brew "findutils"
brew "gnu-sed"
brew "gnu-tar"
brew "grep"
brew "wget"
brew "curl"
brew "ca-certificates"
brew "openssl@3"
brew "tree"
brew "htop"
brew "btop"
brew "watch"
brew "tmux"
brew "zsh-completions"
brew "zsh-autosuggestions"
brew "zsh-syntax-highlighting"
brew "starship"             # prompt (alternative : powerlevel10k)
brew "fzf"
brew "ripgrep"              # rg
brew "fd"
brew "bat"
brew "eza"                  # ls moderne
brew "zoxide"               # cd intelligent
brew "tldr"
brew "direnv"               # variables par projet (.envrc)
brew "jq"
brew "yq"
brew "httpie"
brew "xh"
brew "shellcheck"
brew "shfmt"
brew "pre-commit"
brew "git-delta"            # diff lisible
brew "lazygit"
brew "neovim"
brew "vim"
brew "make"
brew "cmake"
brew "graphviz"
brew "plantuml"
brew "pandoc"
brew "unzip"
brew "p7zip"
brew "mas"                  # App Store CLI

# ============================ Java / JVM ============================
# JDKs gérés par SDKMAN (section 6) — Maven/Gradle aussi, pour gérer les versions
brew "jenv"                 # optionnel si tu préfères jenv à SDKMAN pour le JAVA_HOME

# ============================ Python ============================
brew "pyenv"
brew "pyenv-virtualenv"
brew "uv"                   # gestionnaire Python ultra-rapide
brew "pipx"
brew "poetry"

# ============================ Node ============================
brew "nvm"

# ============================ Data / Big Data ============================
brew "kcat"                 # client Kafka en CLI (ex-kafkacat)
brew "duckdb"               # requêtes SQL locales sur parquet/csv
brew "parquet-cli"
brew "avro-tools"
brew "postgresql@16"        # client psql
brew "mysql-client"
brew "sqlite"
brew "minio/stable/mc"      # client S3 (compatible IBM Cloud Object Storage)
brew "awscli"               # S3 compatible COS (HMAC)

# ============================ Conteneurs / K8s / Cloud ============================
brew "kubernetes-cli"       # kubectl
brew "kubectx"              # kubectx + kubens
brew "k9s"
brew "helm"
brew "stern"                # logs multi-pods
brew "kustomize"
brew "dive"                 # analyse des couches d'image Docker
brew "hadolint"             # lint Dockerfile
brew "trivy"                # scan de vulnérabilités images
brew "skopeo"               # copie/inspection d'images sans daemon
brew "openshift-cli"        # oc (si clusters OpenShift sur IBM Cloud)
brew "terraform"            # si IaC IBM Cloud

# ============================ IA locale ============================
brew "ollama"

# ============================ Proxy authentifiant (si besoin) ============================
# brew "cntlm"

# ============================ Applications (casks) ============================
cask "docker"               # Docker Desktop (si pas déjà déployé par l'IT)
cask "intellij-idea-ce"
cask "iterm2"
cask "visual-studio-code"   # optionnel, pratique pour YAML/Markdown
cask "dbeaver-community"
cask "bruno"                # client API, 100% local (pas de sync cloud → adapté banque)
cask "rectangle"            # gestion des fenêtres
cask "raycast"              # lanceur (alternative Spotlight)
cask "stats"                # CPU/RAM dans la barre de menus
cask "maccy"                # historique presse-papier
cask "font-jetbrains-mono-nerd-font"
cask "font-fira-code-nerd-font"
cask "lm-studio"            # IA locale avec interface (optionnel)
cask "keka"                 # archives
cask "drawio"
```

> ⚠️ Si un cask est bloqué par la politique du poste (installé via Self Service / Jamf), commente-le et utilise le portail interne.

---

## 4. Shell : zsh, Oh My Zsh, `.zshrc` complet

### 4.1 Oh My Zsh

```bash
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" "" --unattended
```

### 4.2 `~/.secrets.zsh` (jamais commité)

```bash
touch ~/.secrets.zsh && chmod 600 ~/.secrets.zsh
```

```bash
# ~/.secrets.zsh — chmod 600
# export NEXUS_USER="mon_uid"
# export NEXUS_TOKEN="..."            # préférer un user token Nexus à ton mot de passe AD
# export GITLAB_TOKEN="glpat-..."     # PAT GitLab (scopes minimaux : read_api, read_repository...)
# export IBMCLOUD_API_KEY="..."       # ou mieux : ibmcloud login --sso
```

### 4.3 `~/.zshrc`

```bash
# =====================================================================
#  ~/.zshrc — MacBook Data Engineer (environnement bancaire avec proxy)
# =====================================================================

# ---------- Homebrew (Apple Silicon) ----------
eval "$(/opt/homebrew/bin/brew shellenv)"

# ---------- Oh My Zsh ----------
export ZSH="$HOME/.oh-my-zsh"
ZSH_THEME=""                       # on utilise starship
plugins=(
  git gitfast docker docker-compose kubectl helm
  mvn gradle sdk python pip pyenv
  fzf direnv history colored-man-pages extract
  macos sudo copypath copyfile
)
fpath+=("$(brew --prefix)/share/zsh-completions")
source "$ZSH/oh-my-zsh.sh"

# ---------- Historique ----------
HISTSIZE=100000
SAVEHIST=100000
HISTFILE=~/.zsh_history
setopt HIST_IGNORE_ALL_DUPS HIST_IGNORE_SPACE SHARE_HISTORY INC_APPEND_HISTORY EXTENDED_HISTORY
# Astuce : une commande précédée d'un espace n'est PAS enregistrée (utile avec un token)

# ---------- Options ----------
setopt AUTO_CD AUTO_PUSHD PUSHD_IGNORE_DUPS CORRECT INTERACTIVE_COMMENTS
export EDITOR="nvim"
export VISUAL="$EDITOR"
export LANG="fr_FR.UTF-8"
export LC_ALL="fr_FR.UTF-8"

# =====================================================================
#  PROXY D'ENTREPRISE
# =====================================================================
export PROXY_HOST="PROXY_HOST"
export PROXY_PORT="PROXY_PORT"
export CORP_NO_PROXY="localhost,127.0.0.1,::1,.local,.mycorp.local,GITLAB_HOST,NEXUS_HOST,host.docker.internal,kubernetes.docker.internal,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16"

proxy_on() {
  export http_proxy="http://${PROXY_HOST}:${PROXY_PORT}"
  export https_proxy="$http_proxy" ftp_proxy="$http_proxy" all_proxy="$http_proxy"
  export HTTP_PROXY="$http_proxy" HTTPS_PROXY="$http_proxy" FTP_PROXY="$http_proxy" ALL_PROXY="$http_proxy"
  export no_proxy="$CORP_NO_PROXY" NO_PROXY="$CORP_NO_PROXY"
  # Java (Maven/Gradle/sbt via JAVA_TOOL_OPTIONS — Maven a aussi son settings.xml)
  export JAVA_PROXY_OPTS="-Dhttp.proxyHost=${PROXY_HOST} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY_HOST} -Dhttps.proxyPort=${PROXY_PORT} -Dhttp.nonProxyHosts=localhost|127.0.0.1|*.mycorp.local"
  echo "🌐 proxy ON  → $http_proxy"
}

proxy_off() {
  unset http_proxy https_proxy ftp_proxy all_proxy HTTP_PROXY HTTPS_PROXY FTP_PROXY ALL_PROXY no_proxy NO_PROXY JAVA_PROXY_OPTS
  echo "🚫 proxy OFF"
}

proxy_status() { env | grep -i _proxy | sort }

proxy_on >/dev/null   # actif par défaut au bureau / VPN — commente si tu bascules souvent

# =====================================================================
#  CERTIFICATS (bundle unique publiques + CA entreprise)
# =====================================================================
export CORP_CA_BUNDLE="$HOME/.certs/ca-bundle.pem"
if [[ -f "$CORP_CA_BUNDLE" ]]; then
  export SSL_CERT_FILE="$CORP_CA_BUNDLE"
  export REQUESTS_CA_BUNDLE="$CORP_CA_BUNDLE"      # Python requests
  export CURL_CA_BUNDLE="$CORP_CA_BUNDLE"
  export PIP_CERT="$CORP_CA_BUNDLE"
  export NODE_EXTRA_CA_CERTS="$CORP_CA_BUNDLE"
  export GIT_SSL_CAINFO="$CORP_CA_BUNDLE"
  export AWS_CA_BUNDLE="$CORP_CA_BUNDLE"           # awscli → IBM COS
fi

# =====================================================================
#  NEXUS
# =====================================================================
export NEXUS_URL="https://NEXUS_HOST"
export PIP_INDEX_URL="${NEXUS_URL}/repository/pypi-proxy/simple"
export PIP_TRUSTED_HOST="NEXUS_HOST"
export UV_INDEX_URL="$PIP_INDEX_URL"
export UV_NATIVE_TLS=true                           # uv utilise le trousseau / bundle système
export POETRY_REPOSITORIES_NEXUS_URL="$PIP_INDEX_URL"

# =====================================================================
#  JAVA (SDKMAN)
# =====================================================================
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC ${JAVA_PROXY_OPTS}"
export GRADLE_OPTS="-Xmx2g ${JAVA_PROXY_OPTS}"

# Bascule rapide de JDK : j8, j11, j17, j21
j()  { sdk use java "$1"; java -version 2>&1 | head -1 }
alias j8='sdk use java $(sdk list java | grep -E "installed|local only" | grep -E " 8\." | awk "{print \$NF}" | head -1)'
alias jls='sdk list java | grep -E "installed|local only"'

# =====================================================================
#  SPARK / HADOOP (local)
# =====================================================================
# SPARK_HOME est géré par SDKMAN (sdk install spark) → $SDKMAN_DIR/candidates/spark/current
export SPARK_HOME="$SDKMAN_DIR/candidates/spark/current"
export HADOOP_HOME="$SDKMAN_DIR/candidates/hadoop/current"
export PYSPARK_PYTHON="python3"
export PYSPARK_DRIVER_PYTHON="python3"
export SPARK_LOCAL_IP="127.0.0.1"
export PATH="$SPARK_HOME/bin:$HADOOP_HOME/bin:$PATH"

# =====================================================================
#  PYTHON
# =====================================================================
export PYENV_ROOT="$HOME/.pyenv"
export PATH="$PYENV_ROOT/bin:$PATH"
eval "$(pyenv init - zsh)"
eval "$(pyenv virtualenv-init -)"
export PIPX_DEFAULT_PYTHON="$(pyenv which python3 2>/dev/null)"
export PYTHONDONTWRITEBYTECODE=1

# =====================================================================
#  NODE
# =====================================================================
export NVM_DIR="$HOME/.nvm"
[[ -s "$(brew --prefix nvm)/nvm.sh" ]] && source "$(brew --prefix nvm)/nvm.sh"

# =====================================================================
#  DOCKER / K8S / IBM CLOUD
# =====================================================================
export DOCKER_DEFAULT_PLATFORM=""          # mets "linux/amd64" si tu builds des images pour IBM Cloud
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
export KUBE_EDITOR="nvim"
export IBMCLOUD_COLOR=true
export IBMCLOUD_VERSION_CHECK=false
alias ic='ibmcloud'
alias icl='ibmcloud login --sso'
alias icks='ibmcloud ks cluster ls'
alias k='kubectl'
alias kx='kubectx'
alias kn='kubens'
alias kgp='kubectl get pods'
alias kl='kubectl logs -f'
alias d='docker'
alias dc='docker compose'
alias dps='docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"'
alias dprune='docker system prune -af --volumes'
alias dbuild-amd='docker buildx build --platform linux/amd64'

# =====================================================================
#  OUTILS MODERNES
# =====================================================================
eval "$(starship init zsh)"
eval "$(zoxide init zsh)"
eval "$(direnv hook zsh)"
source <(fzf --zsh)
source "$(brew --prefix)/share/zsh-autosuggestions/zsh-autosuggestions.zsh"

export FZF_DEFAULT_COMMAND='fd --type f --hidden --exclude .git'
export FZF_CTRL_T_COMMAND="$FZF_DEFAULT_COMMAND"
export BAT_THEME="TwoDark"

alias ls='eza --icons --group-directories-first'
alias ll='eza -lah --icons --git --group-directories-first'
alias lt='eza --tree --level=2 --icons'
alias cat='bat --paging=never'
alias grep='grep --color=auto'
alias vim='nvim'
alias v='nvim'
alias reload='source ~/.zshrc && echo "✅ zshrc rechargé"'
alias zshrc='$EDITOR ~/.zshrc'
alias ports='lsof -iTCP -sTCP:LISTEN -n -P'
alias myip='curl -s ifconfig.me'

# ---------- Git ----------
alias g='git'
alias gs='git status -sb'
alias gl='git log --oneline --graph --decorate -20'
alias gco='git checkout'
alias gsw='git switch'
alias gp='git pull --rebase'
alias gpu='git push -u origin HEAD'
alias lg='lazygit'

# ---------- Maven ----------
alias mci='mvn clean install'
alias mcis='mvn clean install -DskipTests'
alias mcp='mvn clean package -DskipTests'
alias mdt='mvn dependency:tree'
alias mo='mvn -o'                      # offline

# ---------- Kafka (stack docker locale) ----------
alias kt='kcat -b localhost:9092 -L'   # liste topics
kc()  { kcat -b localhost:9092 -C -t "$1" -o beginning -e }   # consomme un topic
kp()  { kcat -b localhost:9092 -P -t "$1" }                   # produit sur un topic

# ---------- Elasticsearch local ----------
alias es='curl -s localhost:9200'
alias eshealth='curl -s localhost:9200/_cluster/health?pretty'
alias esidx='curl -s "localhost:9200/_cat/indices?v&s=index"'

# ---------- Fonctions utiles ----------
mkcd()  { mkdir -p "$1" && cd "$1" }
port()  { lsof -nP -iTCP:"$1" -sTCP:LISTEN }
killport() { lsof -ti tcp:"$1" | xargs kill -9 }
parq()  { duckdb -c "SELECT * FROM read_parquet('$1') LIMIT ${2:-20}" }
parqs() { duckdb -c "DESCRIBE SELECT * FROM read_parquet('$1')" }

# ---------- PATH ----------
export PATH="$HOME/.local/bin:$HOME/bin:$PATH"
typeset -U path   # dédoublonne le PATH

# ---------- Secrets (en dernier) ----------
[[ -f ~/.secrets.zsh ]] && source ~/.secrets.zsh
```

### 4.4 `~/.config/starship.toml` (prompt)

```toml
add_newline = false
format = "$directory$git_branch$git_status$java$python$docker_context$kubernetes$cmd_duration$line_break$character"

[kubernetes]
disabled = false
format = '[⎈ $context($namespace)]($style) '
style = "bold red"            # rouge → tu vois toujours sur quel cluster tu es !

[java]
format = '[☕ $version]($style) '

[python]
format = '[🐍 $version( \($virtualenv\))]($style) '

[cmd_duration]
min_time = 2000
```

---

## 5. Git & GitLab

### 5.1 `~/.gitconfig`

```ini
[user]
    name = Prénom NOM
    email = prenom.nom@mycorp.com
    # signingkey = <ID_GPG>          # si signature exigée
[core]
    editor = nvim
    autocrlf = input
    excludesfile = ~/.gitignore_global
    pager = delta
    longpaths = true
[init]
    defaultBranch = main
[pull]
    rebase = true
[fetch]
    prune = true
[push]
    autoSetupRemote = true
[rebase]
    autoStash = true
[merge]
    conflictstyle = zdiff3
[diff]
    colorMoved = default
[interactive]
    diffFilter = delta --color-only
[delta]
    navigate = true
    side-by-side = true
    line-numbers = true
[http]
    proxy = http://PROXY_HOST:PROXY_PORT
    sslCAInfo = /Users/<toi>/.certs/ca-bundle.pem
# GitLab interne : PAS de proxy
[http "https://GITLAB_HOST/"]
    proxy = ""
[credential]
    helper = osxkeychain
[alias]
    st = status -sb
    lg = log --oneline --graph --decorate --all
    last = log -1 HEAD --stat
    undo = reset --soft HEAD~1
    amend = commit --amend --no-edit
    br = branch -vv
    cleanup = "!git branch --merged | grep -vE '^\\*|main|master|develop' | xargs -n 1 git branch -d"
[commit]
    # gpgsign = true
[lfs]
    # activé si gros fichiers dans les repos
```

### 5.2 `~/.gitignore_global`

```gitignore
.DS_Store
.idea/
*.iml
out/
target/
build/
.gradle/
__pycache__/
*.pyc
.venv/
venv/
.env
.envrc
*.log
.ipynb_checkpoints/
metastore_db/
spark-warehouse/
derby.log
.vscode/
*.swp
```

### 5.3 Clé SSH pour GitLab

```bash
ssh-keygen -t ed25519 -C "prenom.nom@mycorp.com" -f ~/.ssh/id_ed25519_gitlab
pbcopy < ~/.ssh/id_ed25519_gitlab.pub   # → GitLab > Preferences > SSH Keys
```

`~/.ssh/config` :

```sshconfig
Host *
    AddKeysToAgent yes
    UseKeychain yes
    ServerAliveInterval 60

Host GITLAB_HOST
    HostName GITLAB_HOST
    User git
    IdentityFile ~/.ssh/id_ed25519_gitlab
    IdentitiesOnly yes
    # Si le port 22 est bloqué et que GitLab expose SSH sur 443 :
    # Port 443
```

```bash
chmod 700 ~/.ssh && chmod 600 ~/.ssh/config ~/.ssh/id_ed25519_gitlab
ssh -T git@GITLAB_HOST    # → "Welcome to GitLab, @toi!"
```

### 5.4 CLI GitLab (`glab`)

```bash
glab auth login --hostname GITLAB_HOST    # token PAT
glab config set -g host GITLAB_HOST
glab mr list          # tes MR
glab ci status        # statut pipeline
glab ci view          # suivi pipeline en TUI
```

### 5.5 pre-commit (qualité avant push)

```bash
pre-commit install   # dans chaque repo qui a un .pre-commit-config.yaml
```

---

## 6. Java : SDKMAN, Maven, Gradle (via Nexus)

### 6.1 SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

`~/.sdkman/etc/config` :
```properties
sdkman_auto_answer=true
sdkman_auto_env=true         # active les .sdkmanrc par projet
sdkman_colour_enable=true
```

### 6.2 JDKs, outils JVM

> 💡 Aligne-toi **sur les versions de la prod / des clusters**, pas sur la dernière sortie.
> `sdk list java` pour voir les identifiants exacts.

```bash
sdk install java 8.0.xxx-tem       # vieux jobs Hadoop/Spark 2
sdk install java 11.0.xx-tem       # Spark 3.x historique
sdk install java 17.0.xx-tem       # Spark 3.5 / 4.x, Spring Boot 3
sdk install java 21.0.xx-tem       # LTS récente (défaut conseillé si projets compatibles)
sdk default java 17.0.xx-tem

sdk install maven                  # version alignée avec la CI GitLab
sdk install gradle
sdk install scala 2.12.xx          # Spark 3.x → Scala 2.12 / 2.13 selon build
sdk install sbt
sdk install spark 3.5.x            # MÊME version que le cluster IBM (Analytics Engine)
sdk install hadoop 3.3.x
sdk install kotlin                 # optionnel
sdk install jbang                  # scripts Java rapides
sdk install visualvm               # profiling JVM
```

Par projet, fichier `.sdkmanrc` (commité) :
```properties
java=17.0.xx-tem
maven=3.9.x
```

### 6.3 Ajouter la CA d'entreprise aux JDKs (OBLIGATOIRE sinon `PKIX path building failed`)

```bash
for JH in ~/.sdkman/candidates/java/*/; do
  [[ "$JH" == *current* ]] && continue
  KS="$JH/lib/security/cacerts"
  [[ -f "$JH/jre/lib/security/cacerts" ]] && KS="$JH/jre/lib/security/cacerts"   # JDK 8
  sudo keytool -importcert -noprompt -trustcacerts -alias corp-root-ca \
    -file ~/.certs/corp-root-ca.pem -keystore "$KS" -storepass changeit
done
```
> ♻️ À **refaire après chaque nouvelle installation de JDK** (et pour le JBR d'IntelliJ, voir section 13).

### 6.4 `~/.m2/settings.xml` (Nexus + proxy)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">

  <!-- Tout passe par Nexus : un seul miroir -->
  <mirrors>
    <mirror>
      <id>nexus</id>
      <name>Nexus Group</name>
      <mirrorOf>*</mirrorOf>
      <url>https://NEXUS_HOST/repository/maven-public/</url>
    </mirror>
  </mirrors>

  <servers>
    <server>
      <id>nexus</id>
      <username>${env.NEXUS_USER}</username>
      <password>${env.NEXUS_TOKEN}</password>
    </server>
    <server>
      <id>nexus-releases</id>
      <username>${env.NEXUS_USER}</username>
      <password>${env.NEXUS_TOKEN}</password>
    </server>
    <server>
      <id>nexus-snapshots</id>
      <username>${env.NEXUS_USER}</username>
      <password>${env.NEXUS_TOKEN}</password>
    </server>
  </servers>

  <!-- Proxy uniquement pour ce qui sort (Nexus interne exclu) -->
  <proxies>
    <proxy>
      <id>corp-proxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>PROXY_HOST</host>
      <port>PROXY_PORT</port>
      <nonProxyHosts>localhost|127.0.0.1|*.mycorp.local|NEXUS_HOST|GITLAB_HOST</nonProxyHosts>
    </proxy>
  </proxies>

  <profiles>
    <profile>
      <id>nexus</id>
      <repositories>
        <repository>
          <id>central</id><url>http://central</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id><url>http://central</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>nexus</activeProfile>
  </activeProfiles>
</settings>
```

> 🔐 Alternative au token en variable d'env : chiffrement Maven (`mvn --encrypt-master-password` → `~/.m2/settings-security.xml`, puis `mvn --encrypt-password`).

`~/.mavenrc` (optionnel) :
```bash
MAVEN_OPTS="-Xmx2g -XX:+UseG1GC -Djavax.net.ssl.trustStore=$HOME/.certs/corp-truststore.jks"
```

### 6.5 Gradle : `~/.gradle/gradle.properties` + `init.gradle`

```properties
org.gradle.jvmargs=-Xmx3g -XX:+UseG1GC
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
systemProp.http.proxyHost=PROXY_HOST
systemProp.http.proxyPort=PROXY_PORT
systemProp.https.proxyHost=PROXY_HOST
systemProp.https.proxyPort=PROXY_PORT
systemProp.http.nonProxyHosts=localhost|127.0.0.1|*.mycorp.local|NEXUS_HOST
nexusUser=CHANGE_ME
# nexusToken → dans une variable d'env ORG_GRADLE_PROJECT_nexusToken
```

`~/.gradle/init.d/nexus.gradle` :
```groovy
allprojects {
    repositories {
        all { ArtifactRepository repo ->
            if (repo instanceof MavenArtifactRepository && !repo.url.toString().contains('NEXUS_HOST')) {
                remove repo
            }
        }
        maven {
            url 'https://NEXUS_HOST/repository/maven-public/'
            credentials { username = nexusUser; password = System.getenv('NEXUS_TOKEN') }
        }
    }
}
settingsEvaluated { settings ->
    settings.pluginManagement.repositories {
        maven { url 'https://NEXUS_HOST/repository/gradle-plugins/' }
    }
}
```

---

## 7. Python : pyenv / uv / pip via Nexus PyPI

### 7.1 Versions

```bash
pyenv install 3.11      # souvent la version alignée avec PySpark sur les clusters
pyenv install 3.12
pyenv global 3.11
```

> ⚠️ La version Python **driver** et **workers** PySpark doit correspondre à celle du cluster IBM.

### 7.2 `~/.config/pip/pip.conf`

```ini
[global]
index-url = https://NEXUS_HOST/repository/pypi-proxy/simple
trusted-host = NEXUS_HOST
cert = /Users/<toi>/.certs/ca-bundle.pem
timeout = 60
disable-pip-version-check = true
# proxy inutile si Nexus est interne (dans no_proxy)
```

### 7.3 uv : `~/.config/uv/uv.toml`

```toml
native-tls = true

[[index]]
name = "nexus"
url = "https://NEXUS_HOST/repository/pypi-proxy/simple"
default = true
```

### 7.4 Poetry

```bash
poetry config repositories.nexus https://NEXUS_HOST/repository/pypi-proxy/simple
poetry config certificates.nexus.cert ~/.certs/ca-bundle.pem
poetry config virtualenvs.in-project true
poetry config http-basic.nexus "$NEXUS_USER" "$NEXUS_TOKEN"
```

### 7.5 Conda (si utilisé) : `~/.condarc`

```yaml
channels:
  - https://NEXUS_HOST/repository/conda-forge-proxy/
default_channels:
  - https://NEXUS_HOST/repository/conda-proxy/main
ssl_verify: /Users/<toi>/.certs/ca-bundle.pem
channel_priority: strict
auto_activate_base: false
```

### 7.6 Outils Python globaux (pipx)

```bash
pipx install ruff            # lint + format
pipx install black
pipx install mypy
pipx install pre-commit
pipx install ipython
pipx install jupyterlab
pipx install httpie
pipx install sqlfluff        # lint SQL
pipx install cookiecutter
pipx install pip-audit       # vulnérabilités dépendances
```

### 7.7 Projet type data (dans un venv par projet)

```bash
uv venv && source .venv/bin/activate
uv pip install pyspark==<version_cluster> pandas pyarrow polars \
  confluent-kafka elasticsearch==<version_cluster> \
  ibm-cos-sdk ibm-cloud-sdk-core \
  pytest pytest-cov chispa great-expectations jupyter
```

---

## 8. Node.js / npm via Nexus

```bash
mkdir -p ~/.nvm
nvm install --lts && nvm alias default lts/*
```

`~/.npmrc` :
```ini
registry=https://NEXUS_HOST/repository/npm-group/
strict-ssl=true
cafile=/Users/<toi>/.certs/ca-bundle.pem
always-auth=true
# //NEXUS_HOST/repository/npm-group/:_auth=${NPM_AUTH_B64}
```

---

## 9. Docker Desktop (proxy, Nexus, Apple Silicon)

### 9.1 Réglages Docker Desktop (Settings)

| Onglet | Réglage | Valeur conseillée |
|---|---|---|
| General | Use Virtualization framework + **VirtioFS** | ✅ |
| General | Use Rosetta for x86_64/amd64 emulation | ✅ (images amd64 plus rapides) |
| Resources | CPUs | ½ à ⅔ des cœurs |
| Resources | Memory | **8–12 Go** (Spark + Kafka + ES en local sont gourmands) |
| Resources | Swap | 2 Go |
| Resources | Disk image | 100 Go+ |
| Resources > Proxies | Manual | `http://PROXY_HOST:PROXY_PORT` (HTTP & HTTPS), bypass = `CORP_NO_PROXY` |
| Kubernetes | Enable | optionnel (tests de manifests) |

> Si Docker Desktop est géré par l'IT (Settings Management / `admin-settings.json`), certains champs seront grisés : c'est normal.

### 9.2 Daemon : Settings > Docker Engine (JSON)

```json
{
  "registry-mirrors": ["https://NEXUS_DOCKER"],
  "insecure-registries": [],
  "features": { "buildkit": true },
  "builder": { "gc": { "enabled": true, "defaultKeepStorage": "30GB" } },
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" }
}
```

> Le `registry-mirrors` ne fonctionne que pour **Docker Hub**. Pour les autres registres, préfixe explicitement :
> `NEXUS_DOCKER/library/postgres:16`, `NEXUS_DOCKER/elastic/elasticsearch:8.x` (selon la config des repos Nexus).

### 9.3 Proxy transmis aux conteneurs : `~/.docker/config.json`

```json
{
  "proxies": {
    "default": {
      "httpProxy": "http://PROXY_HOST:PROXY_PORT",
      "httpsProxy": "http://PROXY_HOST:PROXY_PORT",
      "noProxy": "localhost,127.0.0.1,.mycorp.local,NEXUS_HOST,host.docker.internal"
    }
  },
  "credsStore": "osxkeychain"
}
```

```bash
docker login NEXUS_DOCKER
docker login icr.io -u iamapikey      # IBM Cloud Container Registry (mot de passe = API key)
```

### 9.4 CA d'entreprise **dans les images** (pip/mvn/apt dans un `docker build`)

```dockerfile
# Dockerfile — à adapter par base image
COPY certs/corp-root-ca.pem /usr/local/share/ca-certificates/corp-root-ca.crt
RUN update-ca-certificates
ENV REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt \
    PIP_INDEX_URL=https://NEXUS_HOST/repository/pypi-proxy/simple
# Java dans l'image :
# RUN keytool -importcert -noprompt -cacerts -storepass changeit -alias corp -file /usr/local/share/ca-certificates/corp-root-ca.crt
```

Build avec proxy :
```bash
docker build \
  --build-arg HTTP_PROXY=$HTTP_PROXY --build-arg HTTPS_PROXY=$HTTPS_PROXY --build-arg NO_PROXY=$NO_PROXY \
  -t mon-app .
```

### 9.5 ⚠️ Apple Silicon (arm64) vs IBM Cloud (amd64)

Ton Mac est **arm64**, les clusters IBM Cloud sont en général **amd64**. Une image buildée « normalement » sur Mac **ne démarrera pas** en prod (`exec format error`).

```bash
docker buildx create --name multi --use
docker buildx build --platform linux/amd64 -t icr.io/<namespace>/mon-app:1.0 --push .
# ou multi-arch :
docker buildx build --platform linux/amd64,linux/arm64 -t ... --push .
```

En pratique : les **images de prod sont buildées par la CI GitLab**, toi tu buildes en local pour tester.

### 9.6 Qualité des images

```bash
hadolint Dockerfile               # lint
trivy image mon-app:latest        # vulnérabilités (DB via proxy ou miroir interne)
dive mon-app:latest               # taille des couches
```

---

## 10. Stack Big Data locale

### 10.1 `docker-compose.yml` (Kafka KRaft + UI + Elasticsearch + Kibana + MinIO)

Range-le dans `~/dev/local-stack/`. Adapte les images au préfixe Nexus et **aux versions de la prod**.

```yaml
name: local-data-stack

services:
  kafka:
    image: apache/kafka:<version_prod>         # ou NEXUS_DOCKER/apache/kafka:...
    container_name: kafka
    ports: ["9092:9092"]
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093,INTERNAL://:29092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,INTERNAL://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list"]
      interval: 10s
      retries: 10

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports: ["8085:8080"]
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
    depends_on: [kafka]

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:<version_prod>
    container_name: elasticsearch
    ports: ["9200:9200"]
    environment:
      discovery.type: single-node
      xpack.security.enabled: "false"           # LOCAL UNIQUEMENT
      ES_JAVA_OPTS: "-Xms1g -Xmx1g"
    volumes: [es-data:/usr/share/elasticsearch/data]

  kibana:
    image: docker.elastic.co/kibana/kibana:<version_prod>
    container_name: kibana
    ports: ["5601:5601"]
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    depends_on: [elasticsearch]

  minio:                                        # simule IBM Cloud Object Storage (S3)
    image: minio/minio:latest
    container_name: minio
    command: server /data --console-address ":9001"
    ports: ["9000:9000", "9001:9001"]
    environment:
      MINIO_ROOT_USER: minio
      MINIO_ROOT_PASSWORD: minio12345
    volumes: [minio-data:/data]

  postgres:                                     # metastore / tests
    image: postgres:16
    container_name: postgres
    ports: ["5432:5432"]
    environment:
      POSTGRES_PASSWORD: postgres
    volumes: [pg-data:/var/lib/postgresql/data]

volumes:
  es-data:
  minio-data:
  pg-data:
```

```bash
docker compose up -d kafka kafka-ui        # démarre juste ce dont tu as besoin
docker compose up -d                       # tout
docker compose down                        # stop (garde les volumes)
```

URLs : Kafka UI → http://localhost:8085 · Kibana → http://localhost:5601 · MinIO → http://localhost:9001

### 10.2 Spark local

```bash
spark-shell --master "local[*]" --driver-memory 4g
pyspark --master "local[*]"
spark-submit --master "local[*]" --class com.mycorp.Job target/job.jar
```

`$SPARK_HOME/conf/spark-defaults.conf` (local) :
```properties
spark.driver.memory                 4g
spark.sql.shuffle.partitions        8
spark.ui.showConsoleProgress        true
spark.sql.session.timeZone          Europe/Paris
spark.serializer                    org.apache.spark.serializer.KryoSerializer
# Lecture S3 (MinIO local / IBM COS) :
# spark.hadoop.fs.s3a.endpoint            http://localhost:9000
# spark.hadoop.fs.s3a.path.style.access   true
# spark.hadoop.fs.s3a.access.key          minio
# spark.hadoop.fs.s3a.secret.key          minio12345
```

> Spark UI locale : http://localhost:4040 pendant l'exécution.
> Les jars `hadoop-aws` / `stocator` (connecteur IBM COS) se récupèrent via `--packages` → passe par ton Nexus (`spark.jars.ivySettings` pointant vers un `ivysettings.xml` Nexus).

`~/.ivy2/ivysettings.xml` (pour `--packages` via Nexus) :
```xml
<ivysettings>
  <settings defaultResolver="nexus"/>
  <resolvers>
    <ibiblio name="nexus" m2compatible="true" root="https://NEXUS_HOST/repository/maven-public/"/>
  </resolvers>
</ivysettings>
```
```properties
# dans spark-defaults.conf
spark.jars.ivySettings  /Users/<toi>/.ivy2/ivysettings.xml
```

---

## 11. IBM Cloud CLI & Kubernetes / OpenShift

### 11.1 Installation

```bash
curl -fsSL https://clis.cloud.ibm.com/install/osx | sh
ibmcloud config --check-version=false
ibmcloud config --usage-stats-collect false
```

### 11.2 Plugins IBM Cloud

```bash
ibmcloud plugin install container-service          # ks : IKS / Red Hat OpenShift on IBM Cloud
ibmcloud plugin install container-registry         # cr : icr.io
ibmcloud plugin install cloud-object-storage       # cos
ibmcloud plugin install analytics-engine-v3        # ae : Spark serverless (IBM Analytics Engine)
ibmcloud plugin install event-streams              # es : Kafka managé (IBM Event Streams)
ibmcloud plugin install cloud-databases            # cdb : Databases for Elasticsearch/PostgreSQL...
ibmcloud plugin install code-engine                # ce
ibmcloud plugin install key-protect                # kp
ibmcloud plugin install secrets-manager            # sm
ibmcloud plugin install vpc-infrastructure         # is
ibmcloud plugin list
ibmcloud plugin update --all
```

> Les noms de plugins peuvent évoluer : `ibmcloud plugin repo-plugins` pour la liste à jour.

### 11.3 Connexion

```bash
ibmcloud login --sso -r eu-de          # SSO entreprise (région à adapter : eu-de / eu-fr2 ...)
ibmcloud target -g <resource-group>
ibmcloud ks cluster ls
ibmcloud ks cluster config --cluster <cluster>     # → kubeconfig
ibmcloud cr login                                  # Docker → icr.io
ibmcloud es init                                   # sélection instance Event Streams
```

> Le proxy est respecté via `HTTPS_PROXY`. Si erreur TLS : vérifie `SSL_CERT_FILE` (section 2).

### 11.4 Kubernetes / OpenShift

```bash
kubectx                    # liste/switch de contextes
kubens <namespace>
k9s                        # TUI : la vraie arme du quotidien
stern <pod-prefix> -n <ns> # logs agrégés
oc login --token=... --server=...   # si OpenShift
helm repo add <nom> https://NEXUS_HOST/repository/helm-proxy/
```

> 🔴 Le prompt starship affiche le contexte k8s en **rouge** : vérifie toujours avant un `kubectl apply/delete`.

---

## 12. Vim / Neovim

### 12.1 Vim classique : `~/.vimrc` + vim-plug

```bash
curl -fLo ~/.vim/autoload/plug.vim --create-dirs \
  https://raw.githubusercontent.com/junegunn/vim-plug/master/plug.vim
```

```vim
" ============================ ~/.vimrc ============================
set nocompatible
filetype plugin indent on
syntax on

call plug#begin('~/.vim/plugged')
" --- Ergonomie ---
Plug 'tpope/vim-sensible'          " defaults sains
Plug 'tpope/vim-surround'          " cs"' ds( ysiw]
Plug 'tpope/vim-commentary'        " gcc / gc
Plug 'tpope/vim-repeat'
Plug 'tpope/vim-unimpaired'
Plug 'jiangmiao/auto-pairs'
Plug 'easymotion/vim-easymotion'
Plug 'mg979/vim-visual-multi'      " multi-curseurs
" --- Navigation ---
Plug 'preservim/nerdtree'
Plug 'junegunn/fzf', { 'do': { -> fzf#install() } }
Plug 'junegunn/fzf.vim'            " :Files :Rg :Buffers
Plug 'christoomey/vim-tmux-navigator'
" --- Git ---
Plug 'tpope/vim-fugitive'
Plug 'airblade/vim-gitgutter'
" --- UI ---
Plug 'vim-airline/vim-airline'
Plug 'vim-airline/vim-airline-themes'
Plug 'morhetz/gruvbox'
Plug 'Yggdroot/indentLine'
" --- Langages / formats data ---
Plug 'sheerun/vim-polyglot'        " Java, Scala, Python, YAML, Dockerfile, SQL, HCL...
Plug 'elzr/vim-json'
Plug 'chrisbra/csv.vim'            " CSV en colonnes
Plug 'ekalinin/Dockerfile.vim'
Plug 'pearofducks/ansible-vim'
Plug 'hashivim/vim-terraform'
Plug 'derekwyatt/vim-scala'
" --- Lint ---
Plug 'dense-analysis/ale'          " shellcheck, hadolint, ruff, yamllint...
call plug#end()

" --- Réglages ---
set number relativenumber
set cursorline
set expandtab tabstop=4 shiftwidth=4 softtabstop=4
set smartindent
set ignorecase smartcase incsearch hlsearch
set hidden
set clipboard=unnamed               " presse-papier macOS
set mouse=a
set undofile undodir=~/.vim/undo
set noswapfile nobackup
set scrolloff=8
set signcolumn=yes
set updatetime=300
set termguicolors
set background=dark
silent! colorscheme gruvbox
autocmd FileType yaml,json,xml,html setlocal ts=2 sw=2 sts=2

let mapleader = " "
nnoremap <leader>e :NERDTreeToggle<CR>
nnoremap <leader>f :Files<CR>
nnoremap <leader>g :Rg<CR>
nnoremap <leader>b :Buffers<CR>
nnoremap <leader>w :w<CR>
nnoremap <leader>q :q<CR>
nnoremap <leader>h :nohlsearch<CR>
nnoremap <leader>gs :Git<CR>

let g:airline_powerline_fonts = 1
let g:ale_fixers = {'python': ['ruff', 'black'], 'sh': ['shfmt'], '*': ['trim_whitespace']}
let g:ale_linters = {'python': ['ruff'], 'sh': ['shellcheck'], 'dockerfile': ['hadolint'], 'yaml': ['yamllint']}
let g:ale_fix_on_save = 1
```

```bash
mkdir -p ~/.vim/undo
vim +PlugInstall +qall
```

### 12.2 Neovim (recommandé) : LazyVim

```bash
mv ~/.config/nvim{,.bak} 2>/dev/null
git clone https://github.com/LazyVim/starter ~/.config/nvim
rm -rf ~/.config/nvim/.git
nvim   # installe tout au premier lancement
```

Dans Neovim, `:LazyExtras` puis active : `lang.java`, `lang.python`, `lang.scala`, `lang.docker`, `lang.yaml`, `lang.json`, `lang.sql`, `lang.markdown`, `lang.terraform`, `util.dot`.
`:Mason` pour les LSP (jdtls, pyright, ruff, yaml-language-server, dockerfile-language-server, bash-language-server).

---

## 13. IntelliJ IDEA Community : config + plugins

> ℹ️ JetBrains a fait évoluer la distribution d'IntelliJ (édition unifiée avec fonctionnalités gratuites / payantes).
> Vérifie quelle édition ton IT déploie : ce guide ne s'appuie **que sur des fonctionnalités gratuites**.
> Les fonctions **Ultimate-only** à NE PAS chercher : Spring avancé, Database Tools intégrés, Kubernetes, Big Data Tools complet, profiler intégré → on compense avec DBeaver, k9s, VisualVM.

### 13.1 Mémoire : Help > Edit Custom VM Options

```properties
-Xms1g
-Xmx6g
-XX:ReservedCodeCacheSize=1024m
-XX:+UseG1GC
-XX:SoftRefLRUPolicyMSPerMB=50
-Dfile.encoding=UTF-8
-Djava.net.preferIPv4Stack=true
```

### 13.2 Proxy & certificats

- **Settings > Appearance & Behavior > System Settings > HTTP Proxy** → *Manual* : `PROXY_HOST:PROXY_PORT`, *No proxy for* : `localhost, 127.0.0.1, *.mycorp.local, NEXUS_HOST, GITLAB_HOST` → *Check connection*.
  (ou *Auto-detect / PAC URL* si l'entreprise fournit un PAC)
- **Settings > Tools > Server Certificates** → ajouter `corp-root-ca.pem` + cocher *Accept non-trusted certificates automatically* **uniquement si autorisé**.
- Le Marketplace des plugins passe par ce proxy. Si l'entreprise a un **dépôt de plugins interne** : *Plugins > ⚙ > Manage Plugin Repositories*.

### 13.3 Build tools

- **Build Tools > Maven** : *Maven home* = SDKMAN (`~/.sdkman/candidates/maven/current`), *User settings file* = `~/.m2/settings.xml` (override ✅), *JDK for importer* = JDK projet, *VM options for importer* = `-Xmx2g`.
- **Maven > Runner** : *Delegate IDE build/run actions to Maven* ❌ (plus rapide), *Skip tests* selon besoin.
- **Build Tools > Gradle** : *Build and run using* = IntelliJ IDEA, *Gradle JVM* = JDK projet.
- **Project Structure > SDKs** : ajoute les JDK SDKMAN (`~/.sdkman/candidates/java/<version>`), Scala SDK, Python SDK (venv du projet).
- **Build, Execution, Deployment > Compiler** : *Build project automatically* ✅, *Compile independent modules in parallel* ✅, heap 2048 Mo.

### 13.4 Éditeur (réglages recommandés)

| Chemin | Réglage |
|---|---|
| Editor > General | *Soft-wrap* : `*.md; *.txt` · *Strip trailing spaces on save* : Modified lines · *Ensure EOF newline* ✅ |
| Editor > General > Auto Import | *Add unambiguous imports on the fly* ✅ · *Optimize imports on the fly* ✅ |
| Editor > General > Code Folding | replier imports ✅ |
| Editor > Font | JetBrains Mono, 14, ligatures ✅ |
| Editor > Code Style | importer le style de l'équipe (`.editorconfig` dans le repo **prioritaire**) ; Java imports : *Class count to use import with '*'* = 99 |
| Editor > File Encodings | UTF-8 partout, *Transparent native-to-ascii* ✅ (properties) |
| Editor > Inlay Hints | paramètres Java ✅ |
| Tools > Actions on Save | *Reformat code* (lignes modifiées), *Optimize imports* ✅ |
| Version Control > Git | *Path* = `/opt/homebrew/bin/git` · *Update method* = Rebase |
| Version Control > Commit | *Use non-modal commit interface* ✅, *Analyze code* ✅ |
| Appearance | Thème New UI, *Compact mode* si petit écran |
| Keymap | **macOS** (ou *IntelliJ IDEA Classic* si habitude), + IdeaVim |

### 13.5 Plugins (Settings > Plugins > Marketplace)

#### ⭐ Indispensables
| Plugin | Pourquoi |
|---|---|
| **IdeaVim** | Modal editing comme dans vim (+ `~/.ideavimrc` ci-dessous) |
| **Scala** | Spark Scala, sbt |
| **Python Community Edition** | PySpark, scripts Python dans IntelliJ CE |
| **Docker** | Dockerfile, docker-compose, gestion conteneurs |
| **GitLab** (bundled) | MR, review dans l'IDE — *Settings > Version Control > GitLab* → `https://GITLAB_HOST` + PAT |
| **GitToolBox** | blame inline, statut branche, fetch auto |
| **SonarQube for IDE** (ex-SonarLint) | qualité / sécurité à la volée — connecte-le au SonarQube interne (*Connected mode*) |
| **CheckStyle-IDEA** | si règles Checkstyle dans la CI |
| **Lombok** (bundled) | si projets Lombok — activer *Annotation processing* |
| **Maven Helper** | conflits de dépendances (crucial avec Spark / Hadoop / Jackson) |
| **.ignore** | `.gitignore`, `.dockerignore` |
| **EnvFile** | charger un `.env` dans les Run Configurations |
| **Key Promoter X** | apprendre les raccourcis |

#### 📊 Data / Big Data
| Plugin | Pourquoi |
|---|---|
| **Avro and Parquet Viewer** | ouvrir `.parquet` / `.avro` directement |
| **CSV Editor** (ou Rainbow CSV) | CSV en tableau / colonnes colorées |
| **Kafka** (plugin JetBrains) | consulter topics/consumers — *vérifier la compatibilité avec ton édition* |
| **JSON Helper / JSON Parser** | formater / valider JSON |
| **Protocol Buffers** | si schemas `.proto` |
| **Jupyter**-like : utilise JupyterLab (pipx) — les notebooks intégrés sont limités en Community |

#### 🧰 Confort
| Plugin | Pourquoi |
|---|---|
| **Rainbow Brackets Lite** | lisibilité |
| **String Manipulation** | camelCase ↔ snake_case, etc. |
| **Grep Console** | coloriser les logs Spark/Spring |
| **Atom Material Icons** | icônes lisibles |
| **Makefile Language** | Makefiles |
| **Terraform and HCL** | IaC IBM Cloud |
| **Kubernetes YAML** : utilise le support YAML + schémas JSON (le plugin Kubernetes complet est Ultimate) |
| **PlantUML Integration** / **Mermaid** | diagrammes d'archi dans les `.md` |
| **Presentation Assistant** | affiche les raccourcis (démos, pair-programming) |
| **MapStruct Support** | si MapStruct |
| **JPA Buddy** | *uniquement si ça tourne dans ton édition* |

#### 🤖 IA locale (voir section 14) — ⚠️ validation RSSI
| Plugin | Usage |
|---|---|
| **Continue** | chat + autocomplétion branchés sur **Ollama local** |
| **ProxyAI** (ex-CodeGPT) | alternative, supporte Ollama / LM Studio |

> ❌ **Pas** d'assistant IA cloud (Copilot, AI Assistant JetBrains cloud, etc.) sur du code bancaire sans accord explicite.

### 13.6 `~/.ideavimrc`

```vim
let mapleader = " "
set number relativenumber
set scrolloff=8
set incsearch hlsearch ignorecase smartcase
set clipboard+=unnamed
set surround
set commentary
set highlightedyank
set NERDTree
set ideajoin

nmap <leader>e :NERDTreeToggle<CR>
nmap <leader>f <Action>(GotoFile)
nmap <leader>g <Action>(FindInPath)
nmap <leader>r <Action>(RenameElement)
nmap <leader>a <Action>(ShowIntentionActions)
nmap <leader>d <Action>(GotoDeclaration)
nmap <leader>i <Action>(GotoImplementation)
nmap <leader>u <Action>(FindUsages)
nmap <leader>t <Action>(RunClass)
nmap <leader>b <Action>(ToggleLineBreakpoint)
nmap <leader>o <Action>(OptimizeImports)
nmap <leader>l <Action>(ReformatCode)
nmap <leader>h :nohlsearch<CR>
nmap gh <Action>(ShowErrorDescription)
nmap ]e <Action>(GotoNextError)
nmap [e <Action>(GotoPreviousError)
```

### 13.7 Run Configurations utiles (Spark local)

- **Application** → Main class du job, *VM options* :
  ```
  -Xmx4g -Dspark.master=local[*] -Dspark.app.name=dev
  --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
  --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED
  ```
  *(les `--add-opens` sont nécessaires pour Spark 3.x/4.x sur JDK 17+)*
- ✅ *Add dependencies with "provided" scope to classpath* (Spark est souvent en `provided`).
- **EnvFile** : `.env` local (jamais commité).

### 13.8 Sauvegarder / synchroniser la config

- *File > Manage IDE Settings > Export Settings* → `settings.zip` dans ton repo dotfiles privé.
- Ou *Settings Sync* si autorisé (sinon export manuel).

---

## 14. IA en local

> 🛡️ **Principe banque** : rien ne sort du poste. Modèles exécutés en local, aucune donnée client / code propriétaire envoyé à un service externe. Valider l'usage avec le RSSI.

### 14.1 Ollama

```bash
brew services start ollama            # démarre le serveur sur 127.0.0.1:11434
# Le téléchargement des modèles passe par le proxy :
launchctl setenv HTTPS_PROXY "http://PROXY_HOST:PROXY_PORT"   # puis redémarrer ollama
brew services restart ollama
```

Variables utiles (dans `.zshrc` ou `launchctl setenv`) :
```bash
export OLLAMA_HOST=127.0.0.1:11434    # n'expose JAMAIS sur 0.0.0.0
export OLLAMA_KEEP_ALIVE=10m
export OLLAMA_FLASH_ATTENTION=1
export OLLAMA_CONTEXT_LENGTH=8192
```

### 14.2 Modèles conseillés selon la RAM du Mac

> Les catalogues bougent vite : vérifie les versions les plus récentes sur la bibliothèque Ollama avant de télécharger.

| RAM | Code (autocomplétion) | Code (chat) | Généraliste | Embeddings (RAG) |
|---|---|---|---|---|
| 16 Go | `qwen2.5-coder:1.5b` | `qwen2.5-coder:7b` | `llama3.1:8b` | `nomic-embed-text` |
| 32 Go | `qwen2.5-coder:3b` | `qwen2.5-coder:14b` | `gpt-oss:20b`, `mistral-small` | `nomic-embed-text` / `mxbai-embed-large` |
| 64 Go+ | `qwen2.5-coder:7b` | `qwen2.5-coder:32b` | modèles 30B+ | `bge-m3` |

```bash
ollama pull qwen2.5-coder:7b
ollama pull qwen2.5-coder:1.5b
ollama pull llama3.1:8b
ollama pull nomic-embed-text
ollama list
ollama run qwen2.5-coder:7b "Explique ce plan d'exécution Spark : ..."
```

> 📦 Si le proxy bloque le registre Ollama : demander à l'IT un miroir interne, ou télécharger les modèles GGUF validés et faire `ollama create mon-modele -f Modelfile`.

### 14.3 Open WebUI (interface type ChatGPT, 100 % locale)

```bash
docker run -d --name open-webui -p 3000:8080 \
  -e OLLAMA_BASE_URL=http://host.docker.internal:11434 \
  -e WEBUI_AUTH=true \
  -e ENABLE_COMMUNITY_SHARING=false \
  -v open-webui:/app/backend/data \
  --restart unless-stopped \
  ghcr.io/open-webui/open-webui:main
```
→ http://localhost:3000 (crée un compte local admin). Supporte le RAG sur tes docs (doc d'archi, runbooks).

### 14.4 Continue (IntelliJ) : `~/.continue/config.yaml`

```yaml
name: local-only
version: 1.0.0
schema: v1
models:
  - name: Qwen Coder 7B (chat)
    provider: ollama
    model: qwen2.5-coder:7b
    apiBase: http://127.0.0.1:11434
    roles: [chat, edit, apply]
  - name: Qwen Coder 1.5B (autocomplete)
    provider: ollama
    model: qwen2.5-coder:1.5b
    apiBase: http://127.0.0.1:11434
    roles: [autocomplete]
  - name: Nomic Embed
    provider: ollama
    model: nomic-embed-text
    apiBase: http://127.0.0.1:11434
    roles: [embed]
```
> Dans les paramètres de Continue, **désactive la télémétrie** (`allowAnonymousTelemetry: false` / option équivalente).

### 14.5 Usage Python (scripts, POC RAG)

```bash
uv pip install ollama langchain langchain-ollama chromadb
```
```python
import ollama
r = ollama.chat(model="qwen2.5-coder:7b",
                messages=[{"role": "user", "content": "Optimise ce job PySpark : ..."}])
print(r["message"]["content"])
```

### 14.6 Cas d'usage concrets data

- Expliquer / optimiser un plan Spark (`df.explain("formatted")` → coller dans le chat).
- Générer des tests unitaires PySpark (chispa) / JUnit.
- Écrire des mappings Elasticsearch, requêtes DSL, schémas Avro.
- Résumer des logs d'erreur YARN / Spark (**après anonymisation**).
- RAG local sur la documentation interne de l'équipe via Open WebUI.

---

## 15. Autres outils utiles

| Outil | Rôle |
|---|---|
| **iTerm2** | terminal (profil : police Nerd Font, *Unlimited scrollback*, `Option` = `Esc+`) |
| **DBeaver Community** | SQL (Postgres, Db2, Hive, Elasticsearch JDBC…) — config proxy dans *Préférences > Réseau* + CA dans *Préférences > Connexions > Sécurité* |
| **Bruno** | tests d'API (collections en fichiers dans le repo, pas de cloud) |
| **JupyterLab** (pipx) | notebooks d'exploration |
| **VisualVM** | profiling JVM (remplace le profiler Ultimate) |
| **Rectangle** | fenêtres : `Ctrl+Opt+←/→` |
| **Raycast** | lanceur + snippets + historique presse-papier |
| **Stats** | surveiller RAM quand Docker + Spark + Ollama tournent |
| **draw.io** | schémas d'archi |
| **direnv** | `.envrc` par projet : `export SPARK_CONF_DIR=$PWD/conf`, `layout python` … |

### iTerm2 : réglages
- *Profiles > Text* : JetBrains Mono Nerd Font 14
- *Profiles > Terminal* : Scrollback illimité
- *Profiles > Keys > Key Mappings* : preset **Natural Text Editing**
- *General > Selection* : *Copy to pasteboard on selection* ✅

### tmux : `~/.tmux.conf` minimal
```tmux
set -g prefix C-a
unbind C-b
set -g mouse on
set -g base-index 1
set -g history-limit 100000
set -g default-terminal "tmux-256color"
bind | split-window -h -c "#{pane_current_path}"
bind - split-window -v -c "#{pane_current_path}"
bind r source-file ~/.tmux.conf \; display "rechargé"
setw -g mode-keys vi
```

---

## 16. Réglages macOS

```bash
# Finder : fichiers cachés, extensions, barre de chemin
defaults write com.apple.finder AppleShowAllFiles -bool true
defaults write NSGlobalDomain AppleShowAllExtensions -bool true
defaults write com.apple.finder ShowPathbar -bool true
defaults write com.apple.finder ShowStatusBar -bool true
defaults write com.apple.finder FXPreferredViewStyle -string "Nlsv"
# Pas de .DS_Store sur les partages réseau / USB
defaults write com.apple.desktopservices DSDontWriteNetworkStores -bool true
defaults write com.apple.desktopservices DSDontWriteUSBStores -bool true
# Clavier rapide (vim !)
defaults write NSGlobalDomain KeyRepeat -int 2
defaults write NSGlobalDomain InitialKeyRepeat -int 15
defaults write NSGlobalDomain ApplePressAndHoldEnabled -bool false
# Désactiver les corrections auto (pénibles en code)
defaults write NSGlobalDomain NSAutomaticSpellingCorrectionEnabled -bool false
defaults write NSGlobalDomain NSAutomaticQuoteSubstitutionEnabled -bool false
defaults write NSGlobalDomain NSAutomaticDashSubstitutionEnabled -bool false
# Captures d'écran dans un dossier dédié
mkdir -p ~/Pictures/Screenshots
defaults write com.apple.screencapture location ~/Pictures/Screenshots
# Dock
defaults write com.apple.dock autohide -bool true
defaults write com.apple.dock show-recents -bool false
killall Finder Dock SystemUIServer
```

Arborescence de travail conseillée :
```text
~/dev/
 ├── gitlab/          # repos de l'équipe (même arbo que les groupes GitLab)
 ├── local-stack/     # docker-compose Kafka/ES/MinIO
 ├── sandbox/         # POC, tests
 └── dotfiles/        # repo PRIVÉ : .zshrc, .gitconfig, Brewfile, .ideavimrc, settings IntelliJ (SANS secrets)
```

> 💡 Mets tout ce README + tes dotfiles dans un repo GitLab **privé** `dotfiles` : la prochaine machine se monte en 1 h.

---

## 17. Script de vérification

`~/dev/dotfiles/verify-setup.sh` :

```bash
#!/usr/bin/env zsh
# Vérifie que l'environnement est opérationnel
ok()   { print -P "%F{green}✔%f $1" }
ko()   { print -P "%F{red}✘%f $1" }
chk()  { command -v "$1" >/dev/null && ok "$1 → $($2 2>&1 | head -1)" || ko "$1 absent" }

echo "== Outils =="
chk brew      "brew --version"
chk git       "git --version"
chk glab      "glab --version"
chk java      "java -version"
chk mvn       "mvn -v"
chk gradle    "gradle -v"
chk scala     "scala -version"
chk spark-submit "spark-submit --version"
chk python3   "python3 --version"
chk uv        "uv --version"
chk node      "node --version"
chk docker    "docker --version"
chk kubectl   "kubectl version --client"
chk helm      "helm version --short"
chk ibmcloud  "ibmcloud --version"
chk kcat      "kcat -V"
chk ollama    "ollama --version"
chk nvim      "nvim --version"

echo "\n== Proxy & certificats =="
[[ -n "$HTTPS_PROXY" ]] && ok "HTTPS_PROXY=$HTTPS_PROXY" || ko "HTTPS_PROXY non défini"
[[ -f "$SSL_CERT_FILE" ]] && ok "SSL_CERT_FILE=$SSL_CERT_FILE" || ko "bundle CA absent"
curl -sSf -o /dev/null https://pypi.org && ok "Internet via proxy (pypi.org)" || ko "Internet via proxy KO"

echo "\n== Interne =="
curl -sSf -o /dev/null "$NEXUS_URL" && ok "Nexus joignable" || ko "Nexus KO"
ssh -o BatchMode=yes -o ConnectTimeout=5 -T git@GITLAB_HOST 2>&1 | grep -qi welcome && ok "GitLab SSH OK" || ko "GitLab SSH KO"
pip download --no-deps -d /tmp/piptest six -q 2>/dev/null && ok "pip via Nexus OK" || ko "pip via Nexus KO"
mvn -q dependency:get -Dartifact=org.apache.commons:commons-lang3:3.14.0 >/dev/null 2>&1 && ok "Maven via Nexus OK" || ko "Maven via Nexus KO"

echo "\n== Docker =="
docker info >/dev/null 2>&1 && ok "Docker daemon OK" || ko "Docker daemon KO"
docker pull -q hello-world >/dev/null 2>&1 && ok "docker pull OK" || ko "docker pull KO (proxy/miroir ?)"

echo "\n== IA locale =="
curl -s http://127.0.0.1:11434/api/tags >/dev/null && ok "Ollama up ($(ollama list | tail -n +2 | wc -l | tr -d ' ') modèles)" || ko "Ollama down"
```

```bash
chmod +x ~/dev/dotfiles/verify-setup.sh && ~/dev/dotfiles/verify-setup.sh
```

---

## 18. Dépannage proxy / SSL

| Symptôme | Cause | Correctif |
|---|---|---|
| `PKIX path building failed` (Maven/IntelliJ/Java) | CA absente du `cacerts` du JDK | §6.3 · pour IntelliJ : §13.2 |
| `SSL: CERTIFICATE_VERIFY_FAILED` (pip/Python) | CA absente du bundle Python | `REQUESTS_CA_BUNDLE`, `PIP_CERT`, `pip.conf cert=` |
| `unable to get local issuer certificate` (git/curl/npm) | idem | `GIT_SSL_CAINFO`, `CURL_CA_BUNDLE`, `NODE_EXTRA_CA_CERTS` |
| `x509: certificate signed by unknown authority` (docker pull) | CA pas dans le trousseau système | §2.1 puis redémarrer Docker Desktop |
| Nexus / GitLab ultra-lents ou en échec | passent par le proxy à tort | ajouter l'hôte dans `NO_PROXY` / `nonProxyHosts` |
| `407 Proxy Authentication Required` | proxy authentifiant | cntlm/px (§2.3) |
| `exec format error` en prod | image arm64 | `docker buildx --platform linux/amd64` (§9.5) |
| Spark : `IllegalAccessError ... sun.nio.ch` | JDK 17+ | `--add-opens` (§13.7) |
| Spark : `Python worker` version mismatch | Python driver ≠ worker | aligner `PYSPARK_PYTHON` / version cluster |
| Kafka local : client se connecte puis timeout | mauvais `advertised.listeners` | depuis le Mac → `localhost:9092` ; depuis un conteneur → `kafka:29092` |
| Elasticsearch ne démarre pas | RAM Docker insuffisante | ≥ 8 Go dans Docker Desktop, `ES_JAVA_OPTS` |
| `ollama pull` bloqué | Ollama ne voit pas le proxy | `launchctl setenv HTTPS_PROXY ...` + restart |

Commandes de diagnostic :
```bash
proxy_status
curl -v https://pypi.org 2>&1 | grep -E "Connected|issuer|SSL"
openssl s_client -connect NEXUS_HOST:443 -showcerts </dev/null | grep -E "s:|i:"
keytool -list -cacerts -storepass changeit | grep -i corp
scutil --proxy                    # proxy système macOS
```

---

## 19. Checklist finale

- [ ] CA d'entreprise : Trousseau ✅ · bundle `~/.certs/ca-bundle.pem` ✅ · JDKs ✅ · IntelliJ ✅
- [ ] Proxy : shell ✅ · git ✅ · Maven ✅ · Gradle ✅ · Docker Desktop ✅ · IntelliJ ✅ · Ollama ✅
- [ ] Nexus : Maven ✅ · PyPI ✅ · npm ✅ · Docker ✅ · Ivy (Spark `--packages`) ✅
- [ ] GitLab : clé SSH ✅ · `glab auth` ✅ · plugin GitLab IntelliJ ✅ · clone d'un repo d'équipe ✅
- [ ] Java : JDK 8/11/17/21 ✅ · `mvn clean install` d'un projet d'équipe OK ✅
- [ ] Python : version cluster ✅ · venv projet ✅ · `pytest` OK ✅
- [ ] Spark local : `spark-shell` OK · version = cluster ✅
- [ ] Docker : stack `local-stack` up (Kafka, ES, MinIO) ✅ · build `linux/amd64` OK ✅
- [ ] IBM Cloud : `ibmcloud login --sso` ✅ · plugins ✅ · `kubectl get ns` sur le cluster dev ✅
- [ ] IntelliJ : VM options ✅ · SDKs ✅ · plugins ✅ · `.ideavimrc` ✅ · settings exportés ✅
- [ ] Vim/Neovim : plugins installés ✅
- [ ] IA locale : Ollama + modèles ✅ · Continue branché ✅ · télémétrie OFF ✅ · validation RSSI ✅
- [ ] `verify-setup.sh` 100 % vert ✅
- [ ] Dotfiles poussés dans un repo GitLab **privé** (sans secrets) ✅

---

*Dernière mise à jour : novembre 2026 — à maintenir à jour à chaque changement d'outil ou de version cluster.*
