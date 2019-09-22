import java.io.File

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Call with Arguments: ServiceName JarPath JarCallArguments")
        return
    }
    val serviceName = args[0]
    val jarPath = args[1]
    val jarArguments = args.drop(2).joinToString(" ")
    val jarFile = File(jarPath)
    if (!jarFile.exists()) {
        println("The jar file does not exist: $jarPath")
        return
    }
    if (!jarFile.isFile) {
        println("The jar file is not a file: $jarPath parsed to ${jarFile.absolutePath}")
    }
    val jarBaseDir = File(jarFile.parent)
    val serviceScriptFile = File(jarBaseDir, "${serviceName}Service.sh")
    val serviceDefinitionFileName = "${serviceName}Service.service"

    val serviceDefinition = """[Unit]
Description = $serviceName Java Service
After network.target = $serviceDefinitionFileName

[Service]
Type = forking
ExecStart = ${serviceScriptFile.absolutePath} start
ExecStop = ${serviceScriptFile.absolutePath} stop
ExecReload = ${serviceScriptFile.absolutePath} restart

[Install]
WantedBy=multi-user.target"""
    val serviceScript = """#!/bin/sh
PID_PATH_NAME=/tmp/${serviceName}-pid
cd ${jarBaseDir.absolutePath}
case ${'$'}1 in
    start)
        echo "Starting $serviceName ..."
        if [ ! -f ${'$'}PID_PATH_NAME ]; then
            nohup java -jar ${jarFile.absolutePath} $jarArguments 2>> /dev/null >> /dev/null &
                        echo ${'$'}! > ${'$'}PID_PATH_NAME
            echo "$serviceName started ..."
        else
            echo "$serviceName is already running ..."
        fi
    ;;
    stop)
        if [ -f ${'$'}PID_PATH_NAME ]; then
            PID=${'$'}(cat ${'$'}PID_PATH_NAME);
            echo "$serviceName stoping ..."
            kill ${'$'}PID;
            echo "$serviceName stopped ..."
            rm ${'$'}PID_PATH_NAME
        else
            echo "$serviceName is not running ..."
        fi
    ;;
    restart)
        if [ -f ${'$'}PID_PATH_NAME ]; then
            PID=${'$'}(cat ${'$'}PID_PATH_NAME);
            echo "$serviceName stopping ...";
            kill ${'$'}PID;
            echo "$serviceName stopped ...";
            rm ${'$'}PID_PATH_NAME
            echo "$serviceName starting ..."
            nohup java -jar ${jarFile.absolutePath} $jarArguments 2>> /dev/null >> /dev/null &
                        echo ${'$'}! > ${'$'}PID_PATH_NAME
            echo "$serviceName started ..."
        else
            echo "$serviceName is not running ..."
        fi
    ;;
esac"""

    val serviceDefinitionFile = File("/etc/systemd/system/", serviceDefinitionFileName)
    println("Writing service definition to ${serviceDefinitionFile.absolutePath}")
    serviceDefinitionFile.writeText(serviceDefinition)
    println("Finished writing service definition")
    println("Writing service script file to ${serviceScriptFile.absolutePath}")
    serviceScriptFile.writeText(serviceScript)
    println("Finished writing service script")

    println("Finished service setup. Use systemctl daemon-reload and then systemctl start ${serviceName}Service to start the service or systemctl enable ${serviceName}Service to install it.")
}