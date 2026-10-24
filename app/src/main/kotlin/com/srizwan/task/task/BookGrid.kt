package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.srizwan.task.AppColors

/**
 * কিতাবের কার্ডের তালিকা (BookGrid.jsx): কভার, সম্পন্ন/বাকি পেইজ ও অগ্রগতির বার,
 * রেট, "শেষ কাজ" ট্যাগ; লোডিং স্কেলিটন।
 *
 * LazyVerticalGrid-এর ভেতরে বসানোর জন্য LazyGridScope এক্সটেনশন
 * (LazyColumn-এর ভেতরে আলাদা LazyVerticalGrid বসালে রানটাইমে ক্র্যাশ করে)।
 */
fun LazyGridScope.bookGridItems(
    status: BooksStatus,
    spinning: Boolean,
    books: List<Book>,
    query: String,
    bookCounts: Map<String, Int>,
    byBook: Map<String, BookSum>,
    resume: Resume?,
    rateOf: (Book?) -> Double,
    onOpen: (String) -> Unit
) {
    val q = query.trim().lowercase()
    val list = books.filter {
        q.isEmpty() || it.name.lowercase().contains(q) || it.author.lowercase().contains(q)
    }
    val skeleton = books.isEmpty() && status == BooksStatus.LOADING
    val failed = books.isEmpty() && status == BooksStatus.ERROR
    val showEmpty = !skeleton && (failed || list.isEmpty())

    if (spinning) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }

    if (skeleton) {
        items(4) { SkeletonCard() }
    } else if (!failed) {
        items(list, key = { it.id }) { b ->
            val done = bookCounts[b.id] ?: 0
            val mine = byBook[b.id]?.n ?: 0
            BookCard(
                book = b,
                done = done,
                total = b.totalPages,
                mine = mine,
                isLast = resume != null && resume.b == b.id,
                rate = rateOf(b),
                onClick = { onOpen(b.id) }
            )
        }
    }

    if (showEmpty) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = if (failed) "কিতাব লোড হয়নি — নেট দেখে রিফ্রেশ চাপুন" else "কোনো কিতাব পাওয়া যায়নি",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.extra.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp)
            )
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    done: Int,
    total: Int,
    mine: Int,
    isLast: Boolean,
    rate: Double,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.extra.line, shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Cover(book.coverUrl)
            if (isLast) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFA8761F), Color(0xFFF2D27A)))
                        )
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "শেষ কাজ",
                        color = Color(0xFF4A3200),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Text(
                text = book.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (book.author.isNotEmpty()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Progress(done, total)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "রেট " + rateTxt(rate) + "/পেইজ" + (if (mine > 0) " • আমার " + bn(mine) else ""),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.extra.gold
            )
        }
    }
}

@Composable
private fun Cover(url: String?) {
    var bad by remember(url) { mutableStateOf(false) }
    if (url.isNullOrBlank() || bad) {
        Text(text = "📖", fontSize = 44.sp)
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = { bad = true }
        )
    }
}

@Composable
private fun Progress(done: Int, total: Int) {
    val ok = AppColors.extra.ok
    val gold = AppColors.extra.gold
    val muted = AppColors.extra.muted
    if (total > 0) {
        val rem = maxOf(0, total - done)
        val pct = (done.toFloat() / total).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = ok, fontWeight = FontWeight.Bold)) { append(bn(done) + " সম্পন্ন") }
                withStyle(SpanStyle(color = muted)) { append(" / ") }
                withStyle(SpanStyle(color = gold, fontWeight = FontWeight.Bold)) { append(bn(rem) + " বাকি") }
            },
            fontSize = 12.5.sp
        )
    } else if (done > 0) {
        // মোট পেইজ জানা না থাকলে "বাকি অজানা"
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = ok, fontWeight = FontWeight.Bold)) { append(bn(done) + " সম্পন্ন") }
                withStyle(SpanStyle(color = muted)) { append(" / বাকি অজানা") }
            },
            fontSize = 12.5.sp
        )
    }
}

@Composable
private fun SkeletonCard() {
    val shape = RoundedCornerShape(14.dp)
    val bar = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.extra.line, shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .background(bar)
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
            Spacer(Modifier.height(9.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
            Spacer(Modifier.height(9.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(bar))
        }
    }
}
