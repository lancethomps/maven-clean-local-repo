include Makefile.common

mkfile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
current_dir := $(shell echo "$(dir $(mkfile_path))" | sed 's,/$$,,g')

SEMVER_REGEX = ^(?P<major>0|[1-9]\d*)\.(?P<minor>0|[1-9]\d*)\.(?P<patch>0|[1-9]\d*)(?:-(?P<prerelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+(?P<buildmetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$$
EXIT_ON_ERROR = set -e;
NO_GENERATE_BACKUP_POMS = -DgenerateBackupPoms=false -DprocessAllModules=true

UNAME_S := $(shell uname -s 2>/dev/null || echo)

TIMESTAMP := $(shell date -u +"%Y%m%dT%H%M%S000Z")

VERSION_CURRENT := $(shell mvn --offline help:evaluate -Dexpression=project.version --quiet -DforceStdout)
VERSION_MAJOR := $(shell python -c 'import re;print(re.fullmatch(r"$(SEMVER_REGEX)", "$(VERSION_CURRENT)").group("major"))')
VERSION_MINOR := $(shell python -c 'import re;print(re.fullmatch(r"$(SEMVER_REGEX)", "$(VERSION_CURRENT)").group("minor"))')
VERSION_PATCH := $(shell python -c 'import re;print(re.fullmatch(r"$(SEMVER_REGEX)", "$(VERSION_CURRENT)").group("patch"))')
VERSION_PRERELEASE_NO_PREFIX := $(shell python -c 'import re;print(re.fullmatch(r"$(SEMVER_REGEX)", "$(VERSION_CURRENT)").group("prerelease") or "")')
VERSION_PRERELEASE := $(shell test -n '$(VERSION_PRERELEASE_NO_PREFIX)' && echo '-$(VERSION_PRERELEASE_NO_PREFIX)' || echo '')
VERSION_MAJOR_NEXT := $(shell echo $$(($(VERSION_MAJOR) + 1)))
VERSION_MINOR_NEXT := $(shell echo $$(($(VERSION_MINOR) + 1)))
VERSION_PATCH_NEXT := $(shell echo $$(($(VERSION_PATCH) + 1)))

.PHONY: echo-version echo-version-current echo-version-current-release echo-version-next-minor
.PHONY: deploy deploy-snapshot deploy-release

####
# Functions
####

echo-version:
	@echo "VERSION_CURRENT                -> $(VERSION_CURRENT)"
	@echo "VERSION_MAJOR                  -> $(VERSION_MAJOR)"
	@echo "VERSION_MINOR                  -> $(VERSION_MINOR)"
	@echo "VERSION_PATCH                  -> $(VERSION_PATCH)"
	@echo "VERSION_PRERELEASE             -> $(VERSION_PRERELEASE)"

echo-version-current:
	@echo "$(VERSION_CURRENT)"

echo-version-current-release:
	@echo "$(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH)"

echo-version-next-major:
	@echo "$(VERSION_MAJOR_NEXT).0.0"

echo-version-next-minor:
	@echo "$(VERSION_MAJOR).$(VERSION_MINOR_NEXT).0"

echo-version-next-patch:
	@echo "$(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH_NEXT)"

version-bump-major:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR_NEXT).0.0$(VERSION_PRERELEASE)

version-bump-major-snapshot:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR_NEXT).0.0-SNAPSHOT

version-bump-minor:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR).$(VERSION_MINOR_NEXT).0$(VERSION_PRERELEASE)

version-bump-minor-snapshot:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR).$(VERSION_MINOR_NEXT).0-SNAPSHOT

version-bump-patch:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH_NEXT)$(VERSION_PRERELEASE)

version-bump-patch-snapshot:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH_NEXT)-SNAPSHOT

version-snapshot:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH)-SNAPSHOT

version-release:
	@$(EXIT_ON_ERROR) mvn versions:set $(NO_GENERATE_BACKUP_POMS) -DnewVersion=$(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH)

deploy:
	@$(EXIT_ON_ERROR) mvn clean deploy -P release,ossrh

deploy-snapshot: deploy

deploy-release: version-release deploy
