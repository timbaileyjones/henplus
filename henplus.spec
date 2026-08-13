# $Id: henplus.spec.in,v 1.10 2008-11-16 04:24:59 magrokosmos Exp $
#
# No longer templated from a .in file - build.xml's "henplus.spec" target
# used to fill in @HENPLUS_VERSION@, but that target is gone along with the
# rest of the Ant build. Keep this version in sync with pom.xml's <version>
# by hand (RPM version strings can't contain "-", so drop any "-SNAPSHOT"
# qualifier).
%define version 0.9.9

Summary: JDBC SQL utility with readline commandline editing
License: GNU General Public License (GPL)
Group: Application/Java
Name: henplus
Prefix: /usr
Packager: Henner Zeller <H.Zeller@acm.org>
Release: 2
Source: https://github.com/timbaileyjones/henplus/archive/refs/heads/master.tar.gz
URL: https://github.com/timbaileyjones/henplus
Version: %{version}
Buildroot: /tmp/henplus
BuildRequires: maven >= 3.6
BuildArchitectures: noarch

%description
A SQL commandline frontend with TAB-completion  and history for any
JDBC aware database. It supports multiple open database sessions in
parallel.

%prep
%setup -q

%build
mvn -q -Dmaven.repo.local=$RPM_BUILD_DIR/.m2-repo package

%install
install -d -m 755 $RPM_BUILD_ROOT%{prefix}/bin
install -d -m 755 $RPM_BUILD_ROOT%{prefix}/share/henplus
install -m 755 bin/henplus $RPM_BUILD_ROOT%{prefix}/bin/henplus
install -m 644 build/henplus.jar $RPM_BUILD_ROOT%{prefix}/share/henplus/henplus.jar
install -m 644 lib/*.jar $RPM_BUILD_ROOT%{prefix}/share/henplus/

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,755)
%{prefix}/bin/henplus
%defattr(-,root,root,-)
%dir %{prefix}/share/henplus
%{prefix}/share/henplus/*.jar
%doc doc/HenPlus.html


