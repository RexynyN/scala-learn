package apollo 

import scala.sys.process._
import java.io.File

object Apollo {        
    def noNeedForThisExisting(): String = {
        val cmd = "uname -a" 
        val output = cmd.!! 
        println(output)
        output
    }

}