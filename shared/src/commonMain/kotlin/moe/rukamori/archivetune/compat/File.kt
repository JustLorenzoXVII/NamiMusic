package moe.rukamori.archivetune.compat

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/** Sostituto multipiattaforma di java.io.File (le operazioni usate da ArchiveTune), su kotlinx-io. */
class File(
    val path: String,
) {
    constructor(parent: File?, child: String) : this(if (parent == null) child else parent.path.trimEnd('/') + "/" + child)

    constructor(parent: String?, child: String) : this(if (parent == null) child else parent.trimEnd('/') + "/" + child)

    private val kPath: Path
        get() = Path(path)

    val name: String
        get() = path.trimEnd('/').substringAfterLast('/')

    val nameWithoutExtension: String
        get() = name.substringBeforeLast('.')

    val extension: String
        get() = name.substringAfterLast('.', "")

    val absolutePath: String
        get() = path

    val absoluteFile: File
        get() = this

    val canonicalPath: String
        get() = path

    val canonicalFile: File
        get() = this

    /** Spazio libero utilizzabile sul volume, in byte. */
    val usableSpace: Long
        get() = platformUsableSpace(path)

    val freeSpace: Long
        get() = platformUsableSpace(path)

    val totalSpace: Long
        get() = platformTotalSpace(path)

    val parent: String?
        get() = path.trimEnd('/').substringBeforeLast('/', "").takeIf { it.isNotEmpty() }

    val parentFile: File?
        get() = parent?.let(::File)

    fun exists(): Boolean = SystemFileSystem.exists(kPath)

    val isDirectory: Boolean
        get() = SystemFileSystem.metadataOrNull(kPath)?.isDirectory == true

    val isFile: Boolean
        get() = SystemFileSystem.metadataOrNull(kPath)?.isRegularFile == true

    fun isDirectory(): Boolean = isDirectory

    fun isFile(): Boolean = isFile

    fun length(): Long = SystemFileSystem.metadataOrNull(kPath)?.size ?: 0L

    fun lastModified(): Long = platformLastModified(path)

    fun mkdirs(): Boolean =
        runCatching {
            SystemFileSystem.createDirectories(kPath)
            true
        }.getOrDefault(false)

    fun mkdir(): Boolean = mkdirs()

    fun delete(): Boolean =
        runCatching {
            SystemFileSystem.delete(kPath, mustExist = false)
            true
        }.getOrDefault(false)

    fun deleteRecursively(): Boolean {
        if (isDirectory) listFiles()?.forEach { it.deleteRecursively() }
        return delete()
    }

    fun listFiles(): Array<File>? =
        if (!isDirectory) {
            null
        } else {
            runCatching { SystemFileSystem.list(kPath).map { File(it.toString()) }.toTypedArray() }.getOrNull()
        }

    fun listFiles(filter: (File) -> Boolean): Array<File>? = listFiles()?.filter(filter)?.toTypedArray()

    fun list(): Array<String>? = listFiles()?.map { it.name }?.toTypedArray()

    fun createNewFile(): Boolean {
        if (exists()) return false
        parentFile?.mkdirs()
        writeBytes(ByteArray(0))
        return true
    }

    fun renameTo(dest: File): Boolean =
        runCatching {
            SystemFileSystem.atomicMove(kPath, dest.kPath)
            true
        }.getOrDefault(false)

    fun readBytes(): ByteArray = SystemFileSystem.source(kPath).buffered().use { it.readByteArray() }

    fun readText(): String = readBytes().decodeToString()

    fun writeBytes(bytes: ByteArray) {
        parentFile?.takeIf { !it.exists() }?.mkdirs()
        SystemFileSystem.sink(kPath).buffered().use { it.write(bytes) }
    }

    fun writeText(text: String) = writeBytes(text.encodeToByteArray())

    fun appendBytes(bytes: ByteArray) {
        SystemFileSystem.sink(kPath, append = true).buffered().use { it.write(bytes) }
    }

    fun appendText(text: String) = appendBytes(text.encodeToByteArray())

    fun walkBottomUp(): Sequence<File> =
        sequence {
            listFiles()?.forEach { yieldAll(it.walkBottomUp()) }
            yield(this@File)
        }

    fun walkTopDown(): Sequence<File> =
        sequence {
            yield(this@File)
            listFiles()?.forEach { yieldAll(it.walkTopDown()) }
        }

    fun resolve(child: String): File = File(this, child)

    override fun toString(): String = path

    override fun equals(other: Any?): Boolean = other is File && other.path == path

    override fun hashCode(): Int = path.hashCode()
}

internal expect fun platformLastModified(path: String): Long

internal expect fun platformUsableSpace(path: String): Long

internal expect fun platformTotalSpace(path: String): Long
