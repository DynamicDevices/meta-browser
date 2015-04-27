FILESEXTRAPATHS_prepend := "${THISDIR}/cef3:${THISDIR}/chromium:"

#
# CEF3 2171 -> Chromium 39.0.2171.95 -> CEFGlue tag v3.2171.2039 (hash 4caf9b2)
#
CHROMIUM_PV = "39.0.2171.95"

require chromium_${CHROMIUM_PV}.bb

DESCRIPTION = "Chromium Embedded Framework"
LICENSE = "BSD"

RDEPENDS_${PN} = "cairo fontconfig fontconfig-utils freetype \
                  pango pciutils pulseaudio \
                 "
#Generic Fonts are needed

SRCREV_tools = "99bcb0e676eb396bcf8e1af3903aa4b578aeeee0"
SRCREV_cef = "9be541ef4ecfe02abeda2457357a8df0c6440788"
SRCREV_egl = "a5b81b7617ba6757802b9b5f8c950034d5f961ec"
SRCREV_FORMAT = "cef_egl_tools"

SRC_URI += "git://bitbucket.org/chromiumembedded/cef.git;protocol=https;destsuffix=${CHROMIUM_P}/cef;branch=${PV};name=cef \
            git://github.com/kuscsik/ozone-egl.git;protocol=https;destsuffix=${CHROMIUM_P}/ui/ozone/platform/egl;branch=master;name=egl \
            git://chromium.googlesource.com/chromium/tools/depot_tools.git;protocol=https;destsuffix=depot_tools;branch=master;name=tools \
	    file://cef_create_projects_disable_gyp.patch \
	    file://cef_gyp_remove_gtkglext_dependency.patch \
	    file://cef_add_arm_atomics.patch \
          "

SRC_URI[md5sum] = "e48b80d6d9a35d37244a2fce29899c0b"
SRC_URI[sha256sum] = "88b865eab915ca667633090beef4302c12ea2f9b5659c9b24f90839d07b8baca"

S = "${WORKDIR}/${CHROMIUM_P}"

do_fetch[vardeps] += "SRCREV_FORMAT SRCREV_cef SRCREV_egl SRCREV_tools"

export CHROMIUM_BUILD_TYPE="Release"

GYP_ARCH_DEFINES_armv7a  += " target_arch=arm"
GYP_ARCH_DEFINES_i586 = " target_arch=ia32"

export GYP_GENERATORS="ninja"
export BUILD_TARGET_ARCH="${TARGET_ARCH}"
export GYP_DEFINES +="${GYP_ARCH_DEFINES}"

inherit gettext

do_configure() {
	GYP_DEFINES="${GYP_DEFINES}" export GYP_DEFINES
	# LD workaround taken from meta-browser
	# replace LD with CXX, to workaround a possible gyp issue?
	export PATH=${WORKDIR}/depot_tools:"$PATH"
	LD="${CXX}" export LD
	CC="${CC}" export CC
	CXX="${CXX}" export CXX
	CC_host="gcc" export CC_host
	CXX_host="g++" export CXX_host
	# End of LD Workaround
	#-----------------------
	# Configure cef
	#------------------------
	cd cef
	./cef_create_projects.sh
	../build/gyp_chromium cef.gyp --depth=.. ${EXTRA_OEGYP}
	cd -
}

# Workaround to disable qa_configure
do_qa_configure() {
	echo "do_qa_configure"
}

do_compile() {
	ninja -C out/${CHROMIUM_BUILD_TYPE} cefsimple chrome_sandbox
}

do_install() {
	install -d ${D}${bindir}
	install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/cefsimple ${D}${bindir}
	if [ -f "${B}/out/${CHROMIUM_BUILD_TYPE}/icudtl.dat" ]; then
		install -m 0644 ${B}/out/${CHROMIUM_BUILD_TYPE}/icudtl.dat ${D}${bindir}
	fi
	install -d ${D}${libdir}
	install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/lib/libcef.so ${D}${libdir}
	if [ -f "${B}/out/${CHROMIUM_BUILD_TYPE}/libosmesa.so" ]; then
		install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/libosmesa.so ${D}${libdir}
	fi
	if [ -f "${B}/out/${CHROMIUM_BUILD_TYPE}/libffmpegsumo.so" ]; then
		install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/libffmpegsumo.so ${D}${libdir}
	fi
	install -d ${D}${bindir}/chrome
	if [ -f "${B}/out/${CHROMIUM_BUILD_TYPE}/cef_100_percent.pak" ]; then
		install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/cef_100_percent.pak ${D}${bindir}/chrome
	fi
	if [ -f "${B}/out/${CHROMIUM_BUILD_TYPE}/cef_200_percent.pak" ]; then
		install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/cef_200_percent.pak ${D}${bindir}/chrome
	fi
	if [ -f "${B}/out/${CHROMIUM_BUILD_TYPE}/cef_resources.pak" ]; then
		install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/cef_resources.pak ${D}${bindir}/chrome
	fi
	if [ -f "${B}/out/${CHROMIUM_BUILD_TYPE}/cef.pak" ]; then
		install -m 0755 ${B}/out/${CHROMIUM_BUILD_TYPE}/cef.pak ${D}${bindir}/chrome
	fi
	install -d ${D}${bindir}/chrome/locales
	install -m 0644 ${B}/out/${CHROMIUM_BUILD_TYPE}/locales/en-US.pak ${D}${bindir}/chrome/locales
	# take care of yocto-way libraries naming (versions)
	cd ${D}${libdir}
	for library in $(find -type f -name '*.so'); do
		startDir="$(pwd)"
		cd "$(dirname "$library")"
		rm -f "$library.0.0.1"
		mv "$library" "$library.0.0.1"
		ln -sf "$library.0.0.1" "$library.0.0"
		ln -sf "$library.0.0" "$library.0"
		ln -sf "$library.0" "$library"
		cd "$startDir"
	done

}

INSANE_SKIP_${PN} = "ldflags"
FILES_${PN} = "${bindir} ${bindir}/chrome/ ${libdir}"
FILES_${PN} += "${bindir}/chrome/*.pak"
FILES_${PN} += "${bindir}/chrome/locales/*.pak"
FILES_${PN}-dbg += "${bindir}/chrome/.debug/ ${libdir}/.debug/"
