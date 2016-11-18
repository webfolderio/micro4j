(function() {

    'use strict';

    var humanFileSize = function(bytes, si) {
        var thresh = si ? 1000 : 1024;
        if (Math.abs(bytes) < thresh) {
            return bytes + ' B';
        }
        var units = si ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'] : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while (Math.abs(bytes) >= thresh && u < units.length - 1);
        return bytes.toFixed(1) + ' ' + units[u];
    };

    var rowFile = function(index, file, progress) {
        var text = `<tr data-upload-index="${index}" data-upload-waiting>
                    <td class="tbl-upload-td-file-name">${file}</td>
                    <td class="tbl-upload-td-progress"><progress value="0" max="100" style="display: none;"></progress></td>
                    <td class="tbl-upload-td-cancel"><a class="btn-cancel-upload btn-dialog"></a></td>
                </tr>`;
        return text;
    };

    var onClickCancelUpload = function(e) {
        var $target = $(e.target);
        var $parent = $target.closest('tr');

        var uploading = $parent.attr('data-upload-uploading') === 'true';

        if (uploading && CURRENT_UPLOAD != null) {
            var $progress = $parent.find('progress');
            $progress.replaceWith('<label style="color: red">' + messageUploadCancel + '</label>');
            cancelUpload();
        }

        $parent.attr('data-upload-cancel', 'true');
        $target.remove();
    };

    var onChangeFile = async function(e) {
        var input = e.target;
        var files = input.files;

        $('.btn-upload').addClass('btn-selected');

        $('#lbl-create-folder-name').remove();

        $('[data-panel]').remove();

        var panel = document.getElementById('template-upload-panel').innerHTML;

        $('#action-panel-container').append(panel);

        $('#tbl-upload tbody tr').remove();

        var uri = getContextPath() + 'action/upload' + getCurrentPath();

        if (uri.charAt(uri.length - 1) !== '/') {
            uri = uri + '/';
        }

        $('#tbl-upload').attr('data-upload-done', 'false');

        for (var k = 0; k < files.length; k++) {

            if (cancelAllUploads) {
                cancelUpload();
                break;
            }

            var file = files[k];
            var name = file.name;
            var size = file.size;

            var shortName = name;
            if (shortName.length > 28) {
                shortName = '...' + shortName.substring(shortName.length - 28);
            }
            var $row = $(rowFile(k, shortName + ' ' + humanFileSize(size, true)));
            $row.find('.btn-cancel-upload').text(messageUploadCancel);

            $('#tbl-upload').append($row);
        }

        while ( isUploading() ) {

            if (cancelAllUploads) {
                break;
            }

            var $waiting = $('#tbl-upload').find('[data-upload-waiting]');

            if ($waiting.length === 0) {
                break;
            }

            var $next = $waiting.first();
            $next.removeAttr('data-upload-waiting');
            $next.attr('data-upload-uploading', 'true');

            var j = parseInt($next.attr('data-upload-index'), 10);

            var file = files[j];
            var name = file.name;
            var size = file.size;

            var chunkSize = 1024 * 1024;
            var chunkCount = Math.ceil(size / chunkSize);

            var $prgrs = $next.find('progress');

            var uploadCount = 0;
            var uploadFail = false;

            var fileAlreadyExist = false;

            var url = uri + name + '?command=exist';

            try {
                await $.ajax({
                    type: 'POST',
                    url: url,
                    processData: false,
                    beforeSend: function(request) {
                        request.setRequestHeader('X-WebFolder-Original-Size', size);
                    }
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    fileAlreadyExist = true;
                    var message = jqXHR.responseText;
                    $next.removeAttr('data-upload-uploading');
                    $next.find('.tbl-upload-td-progress').css('color', 'red');
                    $next.find('progress').replaceWith(message);
                    $next.find('.btn-cancel-upload').remove();
                });
            } catch(err) {
                console.log(err);
            }

            $next.find('progress').css('display', '');

            if (fileAlreadyExist) {
                continue;
            }

            for (var i = 0; i < chunkCount; i++) {

                if (cancelAllUploads) {
                    cancel
                    break;
                }

                if (uploadFail) {
                    break;
                }

                var cancel = 'true' === $next.attr('data-upload-cancel');

                if (cancel) {
                    $next.removeAttr('data-upload-waiting');
                    $next.removeAttr('data-upload-cancel');
                    break;
                }

                var start = i * chunkSize;
                var end = start + chunkSize;
                var lastChunk = i === (chunkCount - 1);
                if (lastChunk) {
                    end = size;
                }

                var blob = file.slice(start, end);
                try {
                    await $.ajax({
                        xhr: function() {
                            var myXhr = $.ajaxSettings.xhr();
                            if(myXhr.upload) {
                                var uploadProgress = function(e) {
                                    if (e.lengthComputable) {
                                        var progress = i + (e.loaded / e.total);
                                        var pr = ((progress * 100) / chunkCount).toFixed(2);
                                        $prgrs.attr('value', pr);
                                        $prgrs.attr('title', pr + '%');
                                        if ( ! isTouchScreen ) {
                                            window.document.title = messageUplading + ' ' + pr + ' %';
                                        }
                                    }
                                };
                                myXhr.upload.addEventListener('progress', uploadProgress, false);
                            }
                            return myXhr;
                        },
                        beforeSend: function(request) {
                            CURRENT_UPLOAD = request;
                            CURRENT_UPLOAD_URL = uri + name;
                            request.setRequestHeader('X-WebFolder-Original-Size', size);
                            request.setRequestHeader('X-WebFolder-Chunk', i);
                            request.setRequestHeader('X-WebFolder-Chunk-Size', chunkSize);
                        },
                        type: 'POST',
                        url: uri + name,
                        data: blob,
                        processData: false
                    }).done(function(data, textStatus, jqXHR) {
                        uploadCount += 1;
                    }).fail(function(jqXHR, textStatus, errorThrown) {
                        uploadFail = true;
                        var message = jqXHR.responseText;
                        $prgrs.replaceWith(message);
                        $prgrs.parent().css('color', 'red');
                        $next.removeAttr('data-upload-uploading');
                    });
                    if (lastChunk && uploadCount === chunkCount) {
                        $next.find('.btn-cancel-upload').remove();
                        $prgrs.replaceWith(messageUploadDone);
                    }
                } catch (err) {
                    console.error(err);
                }
            }

            $next.removeAttr('data-upload-uploading');
        }

        $('#tbl-upload').attr('data-upload-done', 'true');

        if ( ! isTouchScreen ) {
            window.document.title = 'WebFolder';
        }

        $.pjax.reload('#page', { push: false });
    };

    var onClickBtnUpload = function(e) {
        e.preventDefault();

        cancelAllUploads = false;

        if ( isUploading() && ! isUploadDone() ) {
            e.preventDefault();
            return false;
        }

        var disabled = $(e.currentTarget).hasClass('action-button-disabled');

        if (disabled) {
            e.preventDefault();
            return false;
        }

        $('.btn-selected').removeClass('btn-selected');

        $('#input-file').remove();

        var inputFile = `<input id="input-file" type="file" name="file[]" multiple style="display: none;">`;
        $('body').append(inputFile);

        var $file = $('#input-file');
        $file.click();

        return false;
    };

    $(document).on('click', '.btn-upload', onClickBtnUpload);
    $(document).on('click', '.btn-cancel-upload', onClickCancelUpload);
    $(document).on('change', '#input-file', onChangeFile);

})();
